package conecta_escom.controller;

import conecta_escom.model.Alumno;
import conecta_escom.repository.AlumnoRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class AuthController {

    @Autowired
    private AlumnoRepository alumnoRepository;

    // --- ROOT --- 
    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    // --- REGISTRAR ALUMNOS ---
    @GetMapping("/registro-alumnos")
    public String mostrarRegistroAlumnos() {
        return "alumno-registro";
    }

    @PostMapping("/registro-alumnos")
    public String registrarAlumno(
        @RequestParam String nombre,
        @RequestParam String apellidos,
        @RequestParam String boleta,
        @RequestParam String carrera,
        @RequestParam String correo,
        @RequestParam String telefono,
        @RequestParam String password,
        @RequestParam(required = false) MultipartFile imssFile,
        @RequestParam(required = false) MultipartFile credencialFile,  
        Model model) {

        // Verifica si la boleta ya está registrada
        if (alumnoRepository.existsByBoleta(boleta)) {
            model.addAttribute("error", "La boleta ya está registrada");
            return "alumno-registro";
        }

        // Verifica si el correo ya está registrado
        if (alumnoRepository.existsByCorreo(correo)) {
            model.addAttribute("error", "El correo ya está registrado");
            return "alumno-registro";
        }

        // Crea un nuevo objeto Alumno con los datos
        Alumno alumno = new Alumno();
        alumno.setNombre(nombre);
        alumno.setApellidos(apellidos);
        alumno.setBoleta(boleta);
        alumno.setCarrera(carrera);
        alumno.setCorreo(correo);
        alumno.setTelefono(telefono);
        alumno.setPassword(password);

        try {
            if (imssFile != null && !imssFile.isEmpty()) {
                String imssPath = guardarArchivo(imssFile, boleta, "imss_" + boleta);
                alumno.setImssPath(imssPath);
            }

            if (credencialFile != null && !credencialFile.isEmpty()) {
                String credencialPath = guardarArchivo(credencialFile, boleta, "credencial_" + boleta);
                alumno.setCredencialPath(credencialPath);
            }
        } catch (IOException | InvalidPathException e) {
            model.addAttribute("error", "Error al guardar los documentos: " + e.getMessage());
            return "alumno-registro";
        }

        // Guarda el nuevo alumno en la base de datos
        alumnoRepository.save(alumno);

        return "redirect:/login";
    }

    // --- LOGIN ---
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String usuario, @RequestParam String password, HttpSession session, Model model) {

        Alumno alumno = alumnoRepository.findByBoleta(usuario);
        if (alumno != null && alumno.getPassword().equals(password)) {
            session.setAttribute("usuario", alumno); 
            session.setAttribute("rol", "ALUMNO");
            return "redirect:/inicio-alumnos";
        } 
        
        model.addAttribute("error", "Usuario o contraseña incorrectos");
        return "login";
    }

    // --- INICIO ALUMNOS ---
    @GetMapping("/inicio-alumnos")
    public String inicioAlumnos(HttpSession session, Model model) {

        // Recuperamos el usuario y rol de la sesión
        Object usuario = session.getAttribute("usuario");
        String rol = (String) session.getAttribute("rol");

        // Verificamos si la sesión está activa y si el usuario es un alumno
        if (usuario == null || !"ALUMNO".equals(rol)) {
            return "redirect:/login"; 
        }

        // Convierte el objeto usuario a Alumno
        Alumno alumno = (Alumno) usuario;

        // Agregamos el alumno al modelo para mostrar información en la vista
        model.addAttribute("alumno", alumno);

        return "alumno-inicio"; 
    }

    // --- LOGOUT ---
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Destruye la sesión
        return "redirect:/login";
    }

    // --- MÉTODO PARA GUARDAR ARCHIVOS ---
    private String guardarArchivo(MultipartFile archivo, String boleta, String nombreArchivo) throws IOException {

        // Crear carpeta del alumno si no existe
        Path carpetaAlumno = Paths.get("uploads", "alumnos", boleta);
        if (!Files.exists(carpetaAlumno)) {
            Files.createDirectories(carpetaAlumno);
        }

        // Obtener extensión
        String extension = getExtension(archivo.getOriginalFilename());

        // Ruta final del archivo
        Path rutaArchivo = carpetaAlumno.resolve(nombreArchivo + "." + extension);

        // Guardar archivo
        Files.write(rutaArchivo, archivo.getBytes());

        return rutaArchivo.toString();
    }

    // --- MÉTODO PARA OBTENER LA EXTENSIÓN DE UN ARCHIVO ---
    private String getExtension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            return "";
        }
        return nombreOriginal.substring(nombreOriginal.lastIndexOf('.') + 1);
    }
}
