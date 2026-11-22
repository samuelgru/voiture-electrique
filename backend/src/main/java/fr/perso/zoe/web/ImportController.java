package fr.perso.zoe.web;

import fr.perso.zoe.dto.ImportResultDto;
import fr.perso.zoe.service.ImportExcelService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ImportExcelService service;

    public ImportController(ImportExcelService service) {
        this.service = service;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ImportResultDto importer(@RequestParam("fichier") MultipartFile fichier) {
        try {
            return service.importer(fichier.getInputStream());
        } catch (IOException | org.apache.poi.UnsupportedFileFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier Excel (.xlsx) illisible", e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
