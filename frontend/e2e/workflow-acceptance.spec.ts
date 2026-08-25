import {
  expect,
  test,
  type APIRequestContext,
  type Browser,
  type BrowserContext,
  type Page,
} from '@playwright/test'
import { e2eAdmin, e2eDeputy, e2ePresident, loginAs } from './helpers'

const apiBaseURL = process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:18080'

type Credentials = { email: string; password: string }
type RecordSummary = { id: string; status: string }
type AuditLog = {
  action: string
  comment?: string
  previousStatus?: string
  newStatus?: string
}
type Notification = {
  recordId: string
  notificationType: string
  read: boolean
  message: string
}
type NotificationPage = { content: Notification[] }

async function openRolePage(browser: Browser, credentials: Credentials) {
  const context = await browser.newContext()
  const page = await context.newPage()
  await loginAs(page, credentials.email, credentials.password)
  return { context, page }
}

async function createSubmittedRecord(page: Page, title: string, description: string) {
  await page.getByRole('button', { name: 'Yeni Kayıt' }).click()
  const composer = page.getByRole('dialog', { name: 'Yeni Kayıt' })
  await composer.getByLabel(/Başlık/).fill(title)
  await composer.getByLabel(/Kategori/).selectOption({ label: 'Bilgi İşlem' })
  await composer.getByLabel(/Açıklama/).fill(description)

  const createResponsePromise = page.waitForResponse((response) =>
    response.request().method() === 'POST' && response.url().endsWith('/api/records'),
  )
  const actionResponsePromise = page.waitForResponse((response) =>
    response.request().method() === 'POST' && /\/api\/records\/[^/]+\/workflow\/actions$/.test(response.url()),
  )
  await composer.getByRole('button', { name: 'İncelemeye Gönder' }).click()

  const createResponse = await createResponsePromise
  const actionResponse = await actionResponsePromise
  expect(createResponse.ok()).toBe(true)
  expect(actionResponse.ok()).toBe(true)
  const record = await createResponse.json() as RecordSummary
  await expect(page).toHaveURL(new RegExp(`/kayitlar/${record.id}$`))
  return record.id
}

async function performReviewAction({
  page,
  recordId,
  title,
  button,
  dialog,
  confirm,
  comment,
  returnTarget,
}: {
  page: Page
  recordId: string
  title: string
  button: 'Başkana İlet' | 'Geri Gönder' | 'Onayla' | 'Reddet'
  dialog: 'Başkana ilet' | 'Kaydı geri gönder' | 'Kaydı onayla' | 'Kaydı reddet'
  confirm: 'Başkana İlet' | 'Geri Gönder' | 'Onayla' | 'Reddet'
  comment?: string
  returnTarget?: 'Çalışan' | 'Başkan Yardımcısı'
}) {
  await page.goto(`/kayitlar/${recordId}`)
  await expect(page.getByRole('heading', { name: title })).toBeVisible()
  await page.getByRole('button', { name: button }).click()
  const actionDialog = page.getByRole('dialog', { name: dialog })
  if (returnTarget) {
    await actionDialog.getByLabel('Geri gönderilecek kişi').selectOption({ label: returnTarget })
  }
  if (comment !== undefined) {
    await actionDialog.getByRole('textbox').fill(comment)
  }
  const responsePromise = page.waitForResponse((response) =>
    response.request().method() === 'POST' && response.url().includes(`/api/records/${recordId}/workflow/actions`),
  )
  await actionDialog.getByRole('button', { name: confirm }).click()
  expect((await responsePromise).ok()).toBe(true)
}

async function loginApi(request: APIRequestContext, credentials: Credentials) {
  const response = await request.post(`${apiBaseURL}/api/auth/login`, { data: credentials })
  expect(response.ok()).toBe(true)
  const body = await response.json() as { accessToken: string }
  return body.accessToken
}

function authHeaders(accessToken: string) {
  return { Authorization: `Bearer ${accessToken}` }
}

async function getRecord(request: APIRequestContext, accessToken: string, recordId: string) {
  const response = await request.get(`${apiBaseURL}/api/records/${recordId}`, {
    headers: authHeaders(accessToken),
  })
  expect(response.ok()).toBe(true)
  return response.json() as Promise<RecordSummary>
}

async function getAuditLogs(request: APIRequestContext, accessToken: string, recordId: string) {
  const response = await request.get(`${apiBaseURL}/api/audit-logs/record/${recordId}`, {
    headers: authHeaders(accessToken),
  })
  expect(response.ok()).toBe(true)
  return response.json() as Promise<AuditLog[]>
}

async function getNotifications(request: APIRequestContext, accessToken: string) {
  const response = await request.get(`${apiBaseURL}/api/notifications`, {
    headers: authHeaders(accessToken),
    params: { page: 0, size: 100, sort: 'createdAt,desc' },
  })
  expect(response.ok()).toBe(true)
  const body = await response.json() as NotificationPage
  return body.content
}

async function getUnreadNotifications(request: APIRequestContext, accessToken: string) {
  const response = await request.get(`${apiBaseURL}/api/notifications/unread`, {
    headers: authHeaders(accessToken),
  })
  expect(response.ok()).toBe(true)
  return response.json() as Promise<Array<Notification & { id: string }>>
}

async function getUnreadNotificationCount(request: APIRequestContext, accessToken: string) {
  const response = await request.get(`${apiBaseURL}/api/notifications/unread/count`, {
    headers: authHeaders(accessToken),
  })
  expect(response.ok()).toBe(true)
  return response.json() as Promise<number>
}

function expectActionsInOrder(logs: AuditLog[], expectedActions: string[]) {
  const actions = logs.map((item) => item.action)
  let cursor = 0
  for (const action of actions) {
    if (action === expectedActions[cursor]) cursor += 1
  }
  expect(cursor, `İşlem sırası: ${actions.join(' -> ')}`).toBe(expectedActions.length)
}

async function expectLatestNote(page: Page, note: string) {
  const notePanel = page.locator('details').filter({
    has: page.getByRole('heading', { name: 'Son İşlem Notu' }),
  })
  await notePanel.locator('summary').click()
  await expect(notePanel.getByText(note)).toBeVisible()

  const historyPanel = page.locator('details').filter({
    has: page.getByRole('heading', { name: 'İşlem Geçmişi' }),
  })
  await historyPanel.locator('summary').click()
  await expect(historyPanel.getByText(note)).toBeVisible()
}

test('üç kayıt bildirim, filtre, düzeltme, not ve karar dallarında tutarlı ilerler', async ({ browser, request }) => {
  test.setTimeout(150_000)

  const runId = `${Date.now()}`
  const employee = {
    firstName: 'Kabul',
    lastName: `Çalışan ${runId.slice(-5)}`,
    email: `e2e.kabul.${runId}@workflow.test`,
    initialPassword: 'KabulInitial1!',
    password: 'KabulFinal2!',
  }
  const titles = {
    approved: `E2E kabul onay ${runId}`,
    rejected: `E2E kabul ret ${runId}`,
    revision: `E2E kabul düzeltme ${runId}`,
  }
  const notes = {
    deputyReturn: `Yardımcı düzeltme notu ${runId}`,
    deputyForward: `Yardımcı değerlendirmesi ${runId}`,
    presidentReject: `Başkan ret gerekçesi ${runId}`,
    presidentReturn: `Başkan yardımcı değerlendirmesi ${runId}`,
    finalReturn: `Yardımcı son düzeltme notu ${runId}`,
  }
  const contexts: BrowserContext[] = []

  try {
    const admin = await openRolePage(browser, e2eAdmin)
    contexts.push(admin.context)
    await admin.page.goto('/admin/kullanicilar')
    await admin.page.getByRole('button', { name: 'Yeni kullanıcı' }).click()
    const createUserDialog = admin.page.getByRole('dialog', { name: 'Yeni kullanıcı' })
    await createUserDialog.getByLabel('Ad', { exact: true }).fill(employee.firstName)
    await createUserDialog.getByLabel('Soyad', { exact: true }).fill(employee.lastName)
    await createUserDialog.getByLabel('E-posta adresi').fill(employee.email)
    await createUserDialog.getByLabel('İlk giriş şifresi').fill(employee.initialPassword)
    const createUserResponsePromise = admin.page.waitForResponse((response) =>
      response.request().method() === 'POST' && response.url().endsWith('/api/admin/users'),
    )
    await createUserDialog.getByRole('button', { name: 'Kullanıcı Oluştur' }).click()
    expect((await createUserResponsePromise).ok()).toBe(true)
    await expect(admin.page.getByText('Kullanıcı oluşturuldu')).toBeVisible()

    const employeeContext = await browser.newContext()
    contexts.push(employeeContext)
    const employeePage = await employeeContext.newPage()
    await employeePage.goto('/giris')
    await employeePage.getByLabel('E-posta adresi').fill(employee.email)
    await employeePage.getByLabel('Şifre', { exact: true }).fill(employee.initialPassword)
    await employeePage.getByRole('button', { name: 'Giriş Yap' }).click()
    await expect(employeePage).toHaveURL(/\/sifre-degistir$/)
    await expect(employeePage.getByText('İlk giriş güvenliği')).toBeVisible()
    await employeePage.getByLabel('Mevcut şifre', { exact: true }).fill(employee.initialPassword)
    await employeePage.getByLabel('Yeni şifre', { exact: true }).fill(employee.password)
    await employeePage.getByLabel('Yeni şifre tekrar', { exact: true }).fill(employee.password)
    const passwordResponsePromise = employeePage.waitForResponse((response) =>
      response.request().method() === 'POST' && response.url().endsWith('/api/auth/change-password'),
    )
    await employeePage.getByRole('button', { name: 'Şifreyi Güncelle' }).click()
    expect((await passwordResponsePromise).ok()).toBe(true)
    await expect(employeePage).toHaveURL(/\/giris\?reason=password-changed$/)
    await employeePage.getByLabel('E-posta adresi').fill(employee.email)
    await employeePage.getByLabel('Şifre', { exact: true }).fill(employee.password)
    await employeePage.getByRole('button', { name: 'Giriş Yap' }).click()
    await expect(employeePage).toHaveURL(/\/dashboard$/)

    const approvedId = await createSubmittedRecord(employeePage, titles.approved, 'Onaylanacak kabul kaydı.')
    const rejectedId = await createSubmittedRecord(employeePage, titles.rejected, 'Reddedilecek kabul kaydı.')
    const revisionId = await createSubmittedRecord(employeePage, titles.revision, 'Düzeltme döngüsüne girecek kabul kaydı.')

    const deputy = await openRolePage(browser, e2eDeputy)
    contexts.push(deputy.context)
    await deputy.page.goto('/bildirimler')
    for (const recordId of [approvedId, rejectedId, revisionId]) {
      await expect(deputy.page.locator(`a[href="/kayitlar/${recordId}"]`).first()).toBeVisible()
    }

    await deputy.page.goto('/kayitlar')
    await deputy.page.getByLabel('Başlık veya içerikle ara').fill(runId)
    await expect(deputy.page).toHaveURL(new RegExp(`q=${runId}`))
    for (const title of Object.values(titles)) {
      await expect(deputy.page.getByRole('link', { name: title, exact: true })).toBeVisible()
    }
    await deputy.page.getByLabel('Durum').selectOption('BSK_YRD_INCELEMESINDE')
    const recordCount = deputy.page.locator('header').filter({ hasText: 'kayıt bulundu' })
    await expect(recordCount.getByText('3', { exact: true })).toBeVisible()

    await performReviewAction({
      page: deputy.page,
      recordId: approvedId,
      title: titles.approved,
      button: 'Başkana İlet',
      dialog: 'Başkana ilet',
      confirm: 'Başkana İlet',
      comment: notes.deputyForward,
    })
    await performReviewAction({
      page: deputy.page,
      recordId: rejectedId,
      title: titles.rejected,
      button: 'Başkana İlet',
      dialog: 'Başkana ilet',
      confirm: 'Başkana İlet',
      comment: notes.deputyForward,
    })
    await performReviewAction({
      page: deputy.page,
      recordId: revisionId,
      title: titles.revision,
      button: 'Geri Gönder',
      dialog: 'Kaydı geri gönder',
      confirm: 'Geri Gönder',
      comment: notes.deputyReturn,
    })

    await employeePage.goto('/bildirimler')
    const revisionNotificationLink = employeePage.locator(`a[href="/kayitlar/${revisionId}"]`).first()
    await expect(revisionNotificationLink).toBeVisible()
    const markReadResponsePromise = employeePage.waitForResponse((response) =>
      response.request().method() === 'PUT' && /\/api\/notifications\/[^/]+\/read$/.test(response.url()),
    )
    await revisionNotificationLink.click()
    expect((await markReadResponsePromise).ok()).toBe(true)
    await expect(employeePage).toHaveURL(new RegExp(`/kayitlar/${revisionId}$`))
    await expectLatestNote(employeePage, notes.deputyReturn)

    await employeePage.getByRole('link', { name: 'Düzenlemeye Devam Et' }).click()
    await expect(employeePage.getByRole('heading', { name: 'Düzeltmeleri Tamamla' })).toBeVisible()
    await expect(employeePage.getByText(notes.deputyReturn)).toBeVisible()
    await employeePage.getByLabel('Kayıt açıklaması').fill(`Düzeltilmiş açıklama ${runId}`)
    const resendResponsePromise = employeePage.waitForResponse((response) =>
      response.request().method() === 'POST' && response.url().includes(`/api/records/${revisionId}/workflow/actions`),
    )
    await employeePage.getByRole('button', { name: 'Yeniden Gönder' }).click()
    const resendDialog = employeePage.getByRole('dialog', { name: 'Başkan Yardımcısına yeniden gönder' })
    await resendDialog.getByRole('button', { name: 'Yeniden Gönder' }).click()
    expect((await resendResponsePromise).ok()).toBe(true)

    await performReviewAction({
      page: deputy.page,
      recordId: revisionId,
      title: titles.revision,
      button: 'Başkana İlet',
      dialog: 'Başkana ilet',
      confirm: 'Başkana İlet',
      comment: notes.deputyForward,
    })

    const president = await openRolePage(browser, e2ePresident)
    contexts.push(president.context)
    await president.page.goto('/bildirimler')
    for (const recordId of [approvedId, rejectedId, revisionId]) {
      await expect(president.page.locator(`a[href="/kayitlar/${recordId}"]`).first()).toBeVisible()
    }

    await performReviewAction({
      page: president.page,
      recordId: approvedId,
      title: titles.approved,
      button: 'Onayla',
      dialog: 'Kaydı onayla',
      confirm: 'Onayla',
    })
    await performReviewAction({
      page: president.page,
      recordId: rejectedId,
      title: titles.rejected,
      button: 'Reddet',
      dialog: 'Kaydı reddet',
      confirm: 'Reddet',
      comment: notes.presidentReject,
    })
    await performReviewAction({
      page: president.page,
      recordId: revisionId,
      title: titles.revision,
      button: 'Geri Gönder',
      dialog: 'Kaydı geri gönder',
      confirm: 'Geri Gönder',
      comment: notes.presidentReturn,
      returnTarget: 'Başkan Yardımcısı',
    })

    await deputy.page.goto(`/kayitlar/${revisionId}`)
    await expectLatestNote(deputy.page, notes.presidentReturn)
    await performReviewAction({
      page: deputy.page,
      recordId: revisionId,
      title: titles.revision,
      button: 'Geri Gönder',
      dialog: 'Kaydı geri gönder',
      confirm: 'Geri Gönder',
      comment: notes.finalReturn,
    })

    await employeePage.goto(`/kayitlar/${revisionId}`)
    await expectLatestNote(employeePage, notes.finalReturn)

    const employeeToken = await loginApi(request, employee)
    const deputyToken = await loginApi(request, e2eDeputy)
    const presidentToken = await loginApi(request, e2ePresident)

    await expect(getRecord(request, employeeToken, approvedId)).resolves.toMatchObject({ status: 'ONAYLANDI' })
    await expect(getRecord(request, employeeToken, rejectedId)).resolves.toMatchObject({ status: 'REDDEDILDI' })
    await expect(getRecord(request, employeeToken, revisionId)).resolves.toMatchObject({ status: 'DUZENLEME_BEKLIYOR' })

    const approvedLogs = await getAuditLogs(request, employeeToken, approvedId)
    expectActionsInOrder(approvedLogs, ['RECORD_CREATED', 'GONDER', 'BASKANA_ILET', 'ONAYLA'])
    const rejectedLogs = await getAuditLogs(request, employeeToken, rejectedId)
    expectActionsInOrder(rejectedLogs, ['RECORD_CREATED', 'GONDER', 'BASKANA_ILET', 'REDDET'])
    const revisionLogs = await getAuditLogs(request, employeeToken, revisionId)
    expectActionsInOrder(revisionLogs, [
      'RECORD_CREATED',
      'GONDER',
      'CALISANA_GERI_GONDER',
      'RECORD_UPDATED',
      'TEKRAR_GONDER',
      'BASKANA_ILET',
      'BASKAN_YARDIMCISINA_GERI_GONDER',
      'CALISANA_GERI_GONDER',
    ])
    expect(revisionLogs.find((log) => log.comment === notes.deputyReturn)).toMatchObject({
      action: 'CALISANA_GERI_GONDER',
      newStatus: 'DUZENLEME_BEKLIYOR',
    })
    expect(revisionLogs.find((log) => log.comment === notes.presidentReturn)).toMatchObject({
      action: 'BASKAN_YARDIMCISINA_GERI_GONDER',
      newStatus: 'BSK_YRD_INCELEMESINDE',
    })
    expect(revisionLogs.find((log) => log.comment === notes.finalReturn)).toMatchObject({
      action: 'CALISANA_GERI_GONDER',
      newStatus: 'DUZENLEME_BEKLIYOR',
    })

    const employeeNotifications = await getNotifications(request, employeeToken)
    expect(employeeNotifications).toEqual(expect.arrayContaining([
      expect.objectContaining({ recordId: approvedId, notificationType: 'RECORD_APPROVED' }),
      expect.objectContaining({ recordId: rejectedId, notificationType: 'RECORD_REJECTED' }),
      expect.objectContaining({ recordId: revisionId, notificationType: 'RECORD_RETURNED' }),
    ]))
    const deputyNotifications = await getNotifications(request, deputyToken)
    expect(deputyNotifications).toEqual(expect.arrayContaining([
      expect.objectContaining({ recordId: approvedId, notificationType: 'RECORD_SUBMITTED' }),
      expect.objectContaining({ recordId: rejectedId, notificationType: 'RECORD_SUBMITTED' }),
      expect.objectContaining({ recordId: revisionId, notificationType: 'RECORD_RETURNED' }),
    ]))
    const presidentNotifications = await getNotifications(request, presidentToken)
    for (const recordId of [approvedId, rejectedId, revisionId]) {
      expect(presidentNotifications).toEqual(expect.arrayContaining([
        expect.objectContaining({ recordId, notificationType: 'RECORD_FORWARDED' }),
      ]))
    }

    const employeeUnread = await getUnreadNotifications(request, employeeToken)
    expect(employeeUnread.every((notification) => notification.read === false)).toBe(true)
    expect(await getUnreadNotificationCount(request, employeeToken)).toBe(employeeUnread.length)

    const deputyUnread = await getUnreadNotifications(request, deputyToken)
    expect(await getUnreadNotificationCount(request, deputyToken)).toBe(deputyUnread.length)
    const deputyOnlyNotification = deputyUnread[0]
    expect(deputyOnlyNotification).toBeDefined()
    const crossAccountMarkRead = await request.put(
      `${apiBaseURL}/api/notifications/${deputyOnlyNotification.id}/read`,
      { headers: authHeaders(employeeToken) },
    )
    expect(crossAccountMarkRead.status()).toBe(403)
  } finally {
    await Promise.all(contexts.map((context) => context.close()))
  }
})
