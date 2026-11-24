package fr.perso.zoe.web;

import fr.perso.zoe.service.ExportExcelService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExportExcelService service;
    private final ZoneId zone;

    public ExportController(ExportExcelService service, @Value("${zoe.timezone:Europe/Paris}") String zone) {
        this.service = service;
        this.zone = ZoneId.of(zone);
    }

    @GetMapping
    public ResponseEntity<byte[]> exporter() throws IOException {
        byte[] contenu = service.exporter();
        String nom = "zoe-" + LocalDate.now(zone) + ".xlsx";
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nom).build().toString())
                .body(contenu);
    }
}
