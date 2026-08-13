import { readFileSync, readdirSync, writeFileSync } from 'node:fs'

const generatedApiDirectory = new URL('../src/api/generated/', import.meta.url)
const controllerFiles = readdirSync(generatedApiDirectory)
  .filter((fileName) => fileName.endsWith('Controller.ts'))

for (const fileName of controllerFiles) {
  const fileUrl = new URL(fileName, generatedApiDirectory)
  const source = readFileSync(fileUrl, 'utf8')
  const normalizedSource = source
    .replace(
      /import \{([\s\S]*?)\} from "\.\/data-contracts";/,
      'import type {$1} from "./data-contracts";',
    )
    .replace(
      'import { HttpClient, RequestParams } from "./http-client";',
      'import { HttpClient } from "./http-client";\nimport type { RequestParams } from "./http-client";',
    )

  if (normalizedSource.includes('import { HttpClient, RequestParams }')) {
    throw new Error(`${fileName} içindeki HTTP istemcisi importu normalize edilemedi.`)
  }

  if (normalizedSource !== source) writeFileSync(fileUrl, normalizedSource)
}
