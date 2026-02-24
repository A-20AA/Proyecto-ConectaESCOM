package conecta_escom.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadService {

    // Método para guardar un archivo
    public String guardarArchivo(MultipartFile archivo, String tipo, String usuario, String nombreArchivo) throws IOException, InvalidPathException {

        // Crear carpetas si no existen
        Path carpetaAlumno = Paths.get("uploads", tipo, usuario);
        if (!Files.exists(carpetaAlumno)) {
            Files.createDirectories(carpetaAlumno);
        }

        // Obtener extensión
        String extension = getExtension(archivo.getOriginalFilename());

        // Ruta final del archivo (física)
        Path rutaArchivo = carpetaAlumno.resolve(nombreArchivo + "." + extension);

        // Guardar archivo
        Files.write(rutaArchivo, archivo.getBytes());

        // Convertimos la ruta física a un String con formato de URL (/)
        return rutaArchivo.toString().replace("\\", "/");
    }

    // Método para obtener la extensión de un archivo
    private String getExtension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            return "";
        }
        return nombreOriginal.substring(nombreOriginal.lastIndexOf('.') + 1);
    }
}
