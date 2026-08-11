package btk.staj.WorkFlowProject.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class QuickApproveController {

    @GetMapping("/api/records/quick-approve")
    @ResponseBody
    public String quickApprove(@RequestParam UUID recordId, @RequestParam String userEmail) {
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Evrak Onaylandı</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f9; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
                    .card { background: white; padding: 40px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); text-align: center; max-width: 400px; }
                    .icon { font-size: 50px; color: #107c41; margin-bottom: 16px; }
                    h2 { color: #1a1a1a; margin-bottom: 8px; }
                    p { color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="icon">✅</div>
                    <h2>Evrak Başarıyla Onaylandı!</h2>
                    <p>Evrak ID: <b>%s</b></p>
                    <p>İşleminiz veritabanına işlenmiştir. Bu sekmeyi kapatabilirsiniz.</p>
                </div>
            </body>
            </html>
            """.formatted(recordId);
    }
}