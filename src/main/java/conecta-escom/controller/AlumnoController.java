package conecta_escom.controller;

import conecta_escom.model.Alumno;
import conecta_escom.model.Club;
import conecta_escom.repository.AlumnoRepository;
import conecta_escom.repository.ClubRepository;
import conecta_escom.service.FileUploadService;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.List;
import java.util.Set;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/alumno")
public class AlumnoController {

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileUploadService fileUploadService;

    // Método GET que devuelve la página de registro de Alumnos
    @GetMapping("/registro")
    public String mostrarRegistroAlumnos() {
        return "alumno-registro";
    }

    //Método POST para registrar un Alumno
    @PostMapping("/registro")
    public String registrarAlumno(
        @RequestParam String nombre,
        @RequestParam String apellidos,
        @RequestParam String boleta,
        @RequestParam String carrera,
        @RequestParam String correo,
        @RequestParam String telefono,
        @RequestParam String password,
        @RequestParam(required = false) MultipartFile archivoImss,
        @RequestParam(required = false) MultipartFile archivoCredencial,  
        RedirectAttributes redirectAttributes) {

        // Verifica si la boleta ya está registrada
        if (alumnoRepository.existsByBoleta(boleta)) {
            redirectAttributes.addFlashAttribute("error", "La boleta ya está registrada");
            return "redirect:/alumno/registro";
        }

        // Verifica si el correo ya está registrado
        if (alumnoRepository.existsByCorreo(correo)) {
            redirectAttributes.addFlashAttribute("error", "El correo ya está registrado");
            return "redirect:/alumno/registro";
        }

        // Crea un nuevo objeto Alumno con los datos
        Alumno alumno = new Alumno();
        alumno.setNombre(nombre);
        alumno.setApellidos(apellidos);
        alumno.setBoleta(boleta);
        alumno.setCarrera(carrera);
        alumno.setCorreo(correo);
        alumno.setTelefono(telefono);
        alumno.setPassword(passwordEncoder.encode(password));

        try {
            if (archivoImss != null && !archivoImss.isEmpty()) {
                String nombreArchivo = "imss_" + boleta;
                String rutaArchivoImss = fileUploadService.guardarArchivo(archivoImss, "alumnos", boleta, nombreArchivo);
                alumno.setRutaArchivoImss(rutaArchivoImss);
            }

            if (archivoCredencial != null && !archivoCredencial.isEmpty()) {
                String nombreArchivo = "credencial_" + boleta;
                String rutaArchivoCredencial = fileUploadService.guardarArchivo(archivoCredencial, "alumnos", boleta, nombreArchivo);
                alumno.setRutaArchivoCredencial(rutaArchivoCredencial);
            }
        } catch (IOException | InvalidPathException e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar los documentos: " + e.getMessage());
            return "redirect:/alumno/registro";
        }

        // Guarda el nuevo alumno en la base de datos
        alumnoRepository.save(alumno);

        return "redirect:/login";
    }

    // Método GET que devuelve la página principal del Alumno
    @GetMapping("/inicio")
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

        // Cargar el alumno con clubes de manera eficiente
        alumno = alumnoRepository.findByIdWithClubs(alumno.getId());

        // Lógica de la página de inicio para el alumno
        Set<Club> clubesInscritos = alumno.getClubesInscritos();
        int cantidadClubes = clubesInscritos.size();
        List<Club> todosLosClubes = clubRepository.findAll();       
    
        // Agregamos el alumno al modelo para mostrar información en la vista
        model.addAttribute("alumno", alumno);
        model.addAttribute("clubes", clubesInscritos);
        model.addAttribute("cantidadClubes", cantidadClubes);
        model.addAttribute("todosLosClubes", todosLosClubes);

        return "alumno-inicio"; 
    }

    // Método GET que devuelve la página para editar el perfil del Alumno
    @GetMapping("/editar-perfil")
    public String alumnoEditarPerfil(HttpSession session, Model model) {

        // Recuperamos el usuario y rol de la sesión
        Object usuario = session.getAttribute("usuario");
        String rol = (String) session.getAttribute("rol");

        // Verificamos si la sesión está activa y si el usuario es un Alumno
        if (usuario == null || !"ALUMNO".equals(rol)) {
            return "redirect:/login";
        }

        // Convierte el objeto usuario a Alumno
        Alumno alumno = (Alumno) usuario;

        model.addAttribute("alumno", alumno);
           
        return "alumno-editar-perfil";
    }

    //Método POST para actualizar la foto de perfil
    @PostMapping("/editar-perfil/foto")
    public String alumnoActualizarFotoPerfil(
        @RequestParam("id") Integer id, 
        @RequestParam("fotoAlumno") MultipartFile fotoAlumno,
        HttpSession session,
        RedirectAttributes redirectAttributes) {

        // Buscar Alumno por su ID
        Alumno alumno = alumnoRepository.findById(id).orElse(null);

        // Si el Alumno no existe, mostrar un error
        if (alumno == null) {
            redirectAttributes.addFlashAttribute("error", "Alumno no encontrado.");
            return "redirect:/alumno/editar-perfil"; 
        }

        // Actualizar Foto de Perfil del Alumno
        if (fotoAlumno != null && !fotoAlumno.isEmpty()) {
            try {
                String nombreArchivo = "Foto Perfil - " + alumno.getBoleta();
                String rutaFotoPerfil = fileUploadService.guardarArchivo(fotoAlumno, "alumnos", alumno.getBoleta(), nombreArchivo);
                alumno.setRutaFotoPerfil(rutaFotoPerfil);
            } catch (IOException | InvalidPathException e) {
                redirectAttributes.addFlashAttribute("error", "Error al guardar la imagen: " + e.getMessage());
                 return "redirect:/alumno/editar-perfil";
            }
        }

        // Guardar los cambios del Alumno en la base de datos
        alumnoRepository.save(alumno);

        // Actualizar Sesión
        session.setAttribute("usuario", alumno);

        // Mensaje de éxito
        redirectAttributes.addFlashAttribute("success", "Foto de Perfil actualizada");

         return "redirect:/alumno/editar-perfil"; 
    }

    //Método POST para editar los datos del Alumno
    @PostMapping("/editar-perfil/datos")
    public String alumnoActualizarDatos(
        @RequestParam Integer id,
        @RequestParam String nombre,
        @RequestParam String apellidos,
        @RequestParam String correo,
        @RequestParam String telefono,
        @RequestParam String carrera,
        @RequestParam String password,
        @RequestParam(required = false) MultipartFile archivoImss,
        @RequestParam(required = false) MultipartFile archivoCredencial,
        HttpSession session,
        RedirectAttributes redirectAttributes) {

        // Buscar Alumno por su ID
        Alumno alumno = alumnoRepository.findById(id).orElse(null);

        // Si el Alumno no existe, mostrar un error
        if (alumno == null) {
            redirectAttributes.addFlashAttribute("error", "Alumno no encontrado.");
            return "redirect:/alumno/editar-perfil"; 
        }

        // Actualizar los campos del Alumno con los nuevos datos
        alumno.setNombre(nombre);
        alumno.setApellidos(apellidos);
        alumno.setCorreo(correo);
        alumno.setTelefono(telefono);
        alumno.setCarrera(carrera);
        
        // Solo actualizar la contraseña si se proporciona una nueva
        if (password != null && !password.isEmpty()) {
            alumno.setPassword(passwordEncoder.encode(password)); 
        }

        // Actualizar archivo del IMSS si es que se ha subido uno nuevo
        if (archivoImss != null && !archivoImss.isEmpty()) {
            try {
                String nombreArchivo = "imss_" + alumno.getBoleta();
                String rutaArchivoImss = fileUploadService.guardarArchivo(archivoImss, "alumnos", alumno.getBoleta(), nombreArchivo);
                alumno.setRutaArchivoImss(rutaArchivoImss);
            } catch (IOException | InvalidPathException e) {
                redirectAttributes.addFlashAttribute("error", "Error al guardar el archivo: " + e.getMessage());
                return "redirect:/alumno/editar-perfil";
            }
        }

        // Actualizar Credencial si es que se ha subido un nuevo archivo
        if (archivoCredencial != null && !archivoCredencial.isEmpty()) {
            try {
                String nombreArchivo = "credencial_" + alumno.getBoleta();
                String rutaArchivoCredencial = fileUploadService.guardarArchivo(archivoCredencial, "alumnos", alumno.getBoleta(), nombreArchivo);
                alumno.setRutaArchivoCredencial(rutaArchivoCredencial);
            } catch (IOException | InvalidPathException e) {
                redirectAttributes.addFlashAttribute("error", "Error al guardar el archivo: " + e.getMessage());
                return "redirect:/alumno/editar-perfil";
            }
        }

        // Guardar los cambios del Alumno en la base de datos
        alumnoRepository.save(alumno);

        // Actualizar Sesión
        session.setAttribute("usuario", alumno);

        // Mensaje de éxito
        redirectAttributes.addFlashAttribute("success", "Datos actualizados correctamente.");

         return "redirect:/alumno/editar-perfil"; 
    }

    //Método POST para inscribir un Club
    @PostMapping("/inscribir-club/{id}")
    public String inscribirClub(
        @PathVariable Integer id, 
        HttpSession session, 
        RedirectAttributes redirectAttributes) {

        // Recuperamos el usuario (alumno) de la sesión
        Object usuario = session.getAttribute("usuario");
        String rol = (String) session.getAttribute("rol");

        // Verificamos si la sesión está activa y si el usuario es un alumno
        if (usuario == null || !"ALUMNO".equals(rol)) {
            return "redirect:/login";
        }

        // Convierte el objeto usuario a Alumno
        Alumno alumno = (Alumno) usuario;

        // Cargar el alumno con clubes de manera eficiente
        alumno = alumnoRepository.findByIdWithClubs(alumno.getId());

        // Buscar el club por su ID
        Club club = clubRepository.findById(id).orElse(null);

        if (club != null) {
            if (!alumno.getClubesInscritos().contains(club)) {
                alumno.getClubesInscritos().add(club);
                club.getAlumnosInscritos().add(alumno);
                alumnoRepository.save(alumno); 
                clubRepository.save(club);
            }
        }

        redirectAttributes.addFlashAttribute("success", "Te has inscrito correctamente al club " + club.getNombre());

        // Redirigir de nuevo a la página de inicio
        return "redirect:/alumno/inicio";
    }

    // Método GET que devuelve la página para ver los detalles del Club
    @GetMapping("/ver-club/{id}")
    public String detalleClub(
        @PathVariable Integer id,
        HttpSession session,
        Model model) {

        Object usuario = session.getAttribute("usuario");
        String rol = (String) session.getAttribute("rol");

        if (usuario == null || !"ALUMNO".equals(rol)) {
            return "redirect:/login";
        }

        Alumno alumnoSesion = (Alumno) usuario;

        // Cargar alumno con clubes (evita LazyInitializationException)
        Alumno alumno = alumnoRepository.findByIdWithClubs(alumnoSesion.getId());

        Club club = clubRepository.findById(id).orElse(null);
        if (club == null) {
            return "redirect:/alumno/inicio";
        }

        model.addAttribute("alumno", alumno);
        model.addAttribute("club", club);
    
        return "alumno-detalles-club";
    }
}
