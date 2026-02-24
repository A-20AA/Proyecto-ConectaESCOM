package conecta_escom.controller;

import conecta_escom.model.Alumno;
import conecta_escom.model.Club;
import conecta_escom.model.Coordinador;
import conecta_escom.repository.AlumnoRepository;
import conecta_escom.repository.ClubRepository;
import conecta_escom.repository.CoordinadorRepository;
import conecta_escom.service.FileUploadService;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.text.Normalizer;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/administrador")
public class AdministradorController {

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private CoordinadorRepository coordinadorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileUploadService fileUploadService;

    // Página principal del Administrador
    @GetMapping("/inicio")
    public String inicioAdministrador(HttpSession session, Model model) {

        // Recuperamos el usuario y rol de la sesión
        Object usuario = session.getAttribute("usuario");
        String rol = (String) session.getAttribute("rol");

        // Verificamos si la sesión está activa y si el usuario es un administrador
        if (usuario == null || !"ADMINISTRADOR".equals(rol)) {
            return "redirect:/login"; 
        }

        // Lógica de la página de inicio para el administrador
        List<Club> clubes = clubRepository.findAll();
        List<Coordinador> coordinadores = coordinadorRepository.findAll();
        List<Alumno> alumnos = alumnoRepository.findAll();
        long totalClubes = clubRepository.count();
        long totalCoordinadores = coordinadorRepository.count();
        long totalAlumnos = alumnoRepository.count();

        // Agregamos los datos al modelo
        model.addAttribute("clubes", clubes);
        model.addAttribute("coordinadores", coordinadores);
        model.addAttribute("alumnos", alumnos);
        model.addAttribute("totalClubes", totalClubes);
        model.addAttribute("totalCoordinadores", totalCoordinadores);
        model.addAttribute("totalAlumnos", totalAlumnos);

        // Retornamos la vista para la página de inicio del administrador       
        return "administrador-inicio";
    }

    //Método POST para registrar un club
    @PostMapping("/registrar-club")
    public String registrarClub(
        @RequestParam String nombre,
        @RequestParam String tipo,
        @RequestParam String objetivo,
        @RequestParam String ubicacion,
        @RequestParam String horario,
        @RequestParam(required = false) String usuarioCoordinador,
        @RequestParam(required = false) String boletaPresidente,
        @RequestParam(required = false) MultipartFile archivoCronograma,
        RedirectAttributes redirectAttributes) {

        Coordinador coordinador = null;
        Alumno presidente = null;

        // Verificar si el coordinador fue proporcionado
        if (usuarioCoordinador != null && !usuarioCoordinador.trim().isEmpty()) {
            // Verificar si el coordinador existe en la base de datos
            coordinador = coordinadorRepository.findByUsuario(usuarioCoordinador);
            if (coordinador == null) {
                redirectAttributes.addFlashAttribute("error", "Coordinador no encontrado.");
                return "redirect:/administrador/inicio"; 
            }
        }

        // Verificar si el presidente fue proporcionado
        if (boletaPresidente != null && !boletaPresidente.trim().isEmpty()) {
            // Verificar si el presidente existe en la base de datos
            presidente = alumnoRepository.findByBoleta(boletaPresidente);
            if (presidente == null) {
                redirectAttributes.addFlashAttribute("error", "Alumno no encontrado.");
                return "redirect:/administrador/inicio";
            }
        }

        // Crear el nuevo club
        Club club = new Club();
        club.setNombre(nombre);
        club.setClave(obtenerClave(nombre));
        club.setInicialesClub(obtenerInicialesClub(nombre));
        club.setTipo(tipo);
        club.setObjetivo(objetivo);
        club.setUbicacion(ubicacion);
        club.setHorario(horario);
        club.setCoordinadorClub(coordinador);
        club.setPresidenteClub(presidente);

        // Guardar el cronograma si es que se ha subido
        if (archivoCronograma != null && !archivoCronograma.isEmpty()) {
            try {
                String nombreArchivo = "Cronograma - " + nombre;
                String rutaArchivoCronograma = fileUploadService.guardarArchivo(archivoCronograma, "clubes", nombre, nombreArchivo); 
                club.setRutaArchivoCronograma(rutaArchivoCronograma);
            } catch (IOException | InvalidPathException e) {
                redirectAttributes.addFlashAttribute("error", "Error al guardar el cronograma: " + e.getMessage());
                return "redirect:/administrador/inicio";
            }
        } 

        // Guardar el club en la base de datos
        clubRepository.save(club);

        // Mensaje de éxito
        redirectAttributes.addFlashAttribute("success", "Club registrado con éxito.");

        // Redirigir al inicio del administrador
        return "redirect:/administrador/inicio";
    }

    // Método POST para registrar un coordinador
    @PostMapping("/registrar-coordinador")
    public String registrarCoordinador(
        @RequestParam String nombre,
        @RequestParam String apellidos,
        @RequestParam String usuario,
        @RequestParam String password,
        @RequestParam String correo,
        @RequestParam String telefono,
        @RequestParam(required = false) String[] clubes,
        RedirectAttributes redirectAttributes) {

        // Verificar si el usuario ya está registrado
        if (coordinadorRepository.existsByUsuario(usuario)) {
            redirectAttributes.addFlashAttribute("error", "Este nombre de usuario ya está registrado");
            return "redirect:/administrador/inicio";
        }

        // Verificar si el correo ya está registrado
        if (coordinadorRepository.existsByCorreo(correo)) {
            redirectAttributes.addFlashAttribute("error", "Este correo ya está registrado");
            return "redirect:/administrador/inicio"; 
        }

        // Crear el nuevo coordinador
        Coordinador coordinador = new Coordinador();
        coordinador.setNombre(nombre);
        coordinador.setApellidos(apellidos);
        coordinador.setUsuario(usuario);
        coordinador.setPassword(passwordEncoder.encode(password));
        coordinador.setCorreo(correo);
        coordinador.setTelefono(telefono);

        // Procesar los clubes seleccionados
        Set<Club> clubesAsignados = new HashSet<>();
        Set<String> clubesNoAsignados = new HashSet<>(); 
        if (clubes != null) {
            for (String nombreClub : clubes) {

                // Eliminamos acentos, espacios en blanco y convertimos a letras minúsculas
                String claveNombreClub = obtenerClave(nombreClub);
                
                // Buscamos el club 
                Club clubExistente = clubRepository.findByClave(claveNombreClub);

                // Comparamos los valores 
                if (clubExistente != null) {
                    clubExistente.setCoordinadorClub(coordinador);
                    clubesAsignados.add(clubExistente);
                } else {
                    clubesNoAsignados.add(nombreClub);
                }
            }
        }

        // Asignamos los clubes al coordinador
        coordinador.setClubesAsignados(clubesAsignados);

        // Guardar el coordinador en la base de datos
        coordinadorRepository.save(coordinador);

        // Guardar los clubes con su coordinador asignado
        clubRepository.saveAll(clubesAsignados);

        // Enviamos los mensajes adecuados a la vista
        if (clubesNoAsignados.isEmpty()) {
            redirectAttributes.addFlashAttribute("success", "Coordinador registrado con éxito. Todos los clubes fueron asignados.");
        } else {
            redirectAttributes.addFlashAttribute("warning", "Coordinador registrado con éxito, pero los siguientes clubes no pudieron ser asignados: " 
            + String.join(", ", clubesNoAsignados));
        }

        // Redirigir al inicio del administrador
        return "redirect:/administrador/inicio"; 
    }

    // Mostrar formulario de edición para un club
    @GetMapping("/editar-club/{id}")
    public String mostrarFormularioEditarClub(
        @PathVariable Integer id, 
        RedirectAttributes redirectAttributes, 
        Model model) {
    
        // Buscar club por su ID
        Club club = clubRepository.findById(id).orElse(null);

        // Si no se encuentra el club, redirigir con un mensaje de error
        if (club == null) {
            redirectAttributes.addFlashAttribute("error", "Club no encontrado.");
            return "redirect:/administrador/inicio";
        }

        // Agregar el club encontrado al modelo para que se pueda usar en la vista
        model.addAttribute("club", club);

        // Retornar la vista de edición para el club
        return "administrador-editar-club"; 
    }

    //Método POST para editar los datos de un club
    @PostMapping("/editar-club")
    public String editarClub(
        @RequestParam Integer id,
        @RequestParam String nombre,
        @RequestParam String tipo,
        @RequestParam String objetivo,
        @RequestParam String ubicacion,
        @RequestParam String horario,
        @RequestParam String usuarioCoordinador,
        @RequestParam String boletaPresidente,
        @RequestParam(required = false) MultipartFile archivoCronograma,
        RedirectAttributes redirectAttributes) {

        // Buscar club por su ID
        Club club = clubRepository.findById(id).orElse(null);

        // Si el club no existe, mostrar un error
        if (club == null) {
            redirectAttributes.addFlashAttribute("error", "Club no encontrado.");
            return "redirect:/administrador/inicio"; 
        }

        Coordinador coordinador = null;
        Alumno presidente = null;

        // Verificar si el coordinador fue proporcionado
        if (usuarioCoordinador != null && !usuarioCoordinador.trim().isEmpty()) {
            // Verificar si el coordinador existe en la base de datos
            coordinador = coordinadorRepository.findByUsuario(usuarioCoordinador);
            if (coordinador == null) {
                redirectAttributes.addFlashAttribute("error", "Coordinador no encontrado.");
                return "redirect:/administrador/editar-club"; 
            }
        }

        // Verificar si el presidente fue proporcionado
        if (boletaPresidente != null && !boletaPresidente.trim().isEmpty()) {
            // Verificar si el presidente existe en la base de datos
            presidente = alumnoRepository.findByBoleta(boletaPresidente);
            if (presidente == null) {
                redirectAttributes.addFlashAttribute("error", "Alumno no encontrado.");
                return "redirect:/administrador/editar-club";
            }
        }

        // Actualizar los campos del club con los nuevos datos
        club.setNombre(nombre);
        club.setClave(obtenerClave(nombre));
        club.setInicialesClub(obtenerInicialesClub(nombre));
        club.setTipo(tipo);
        club.setObjetivo(objetivo);
        club.setUbicacion(ubicacion);
        club.setHorario(horario);
        club.setCoordinadorClub(coordinador);
        club.setPresidenteClub(presidente);

        // Actualizar el cronograma si es que se ha subido un nuevo archivo
        if (archivoCronograma != null && !archivoCronograma.isEmpty()) {
            try {
                String nombreArchivo = "Cronograma - " + nombre;
                String rutaArchivoCronograma = fileUploadService.guardarArchivo(archivoCronograma, "clubes", nombre, nombreArchivo);
                club.setRutaArchivoCronograma(rutaArchivoCronograma);
            } catch (IOException | InvalidPathException e) {
                redirectAttributes.addFlashAttribute("error", "Error al guardar el cronograma: " + e.getMessage());
                return "redirect:/administrador/editar-club";
            }
        }

        // Guardar los cambios del club en la base de datos
        clubRepository.save(club);

        // Mensaje de éxito
        redirectAttributes.addFlashAttribute("success", "Club actualizado correctamente.");

        // Redirigir al inicio del administrador
        return "redirect:/administrador/inicio"; 
    }

    // Mostrar formulario de edición para un coordinador
    @GetMapping("/editar-coordinador/{id}")
    public String mostrarFormularioEditarCoordinador(
        @PathVariable Integer id, 
        RedirectAttributes redirectAttributes, 
        Model model) {
    
        // Buscar coordinador por su ID
        Coordinador coordinador = coordinadorRepository.findById(id).orElse(null);

        // Si no se encuentra el coordinador, redirigir con un mensaje de error
        if (coordinador == null) {
            redirectAttributes.addFlashAttribute("error", "Coordinador no encontrado.");
            return "redirect:/administrador/inicio";
        }

        // Agregar el coordinador encontrado al modelo para que se pueda usar en la vista
        model.addAttribute("coordinador", coordinador);

        // Retornar la vista de edición para el coordinador
        return "administrador-editar-coordinador"; 
    }

    //Método POST para editar los datos de un coordinador
    @PostMapping("/editar-coordinador")
    public String editarCoordinador(
        @RequestParam Integer id,  
        @RequestParam String nombre,
        @RequestParam String apellidos,
        @RequestParam String usuario,
        @RequestParam String password,
        @RequestParam String correo,
        @RequestParam String telefono,
        @RequestParam(required = false) String[] clubes,
        RedirectAttributes redirectAttributes) {

        // Buscar coordinador por su ID
        Coordinador coordinador = coordinadorRepository.findById(id).orElse(null);
    
        if (coordinador == null) {
            redirectAttributes.addFlashAttribute("error", "Coordinador no encontrado.");
            return "redirect:/administrador/inicio";
        }

        // Verificar si el usuario ya está registrado (excepto si es el mismo usuario)
        if (!coordinador.getUsuario().equals(usuario) && coordinadorRepository.existsByUsuario(usuario)) {
            redirectAttributes.addFlashAttribute("error", "Este nombre de usuario ya está registrado.");
            return "redirect:/administrador/inicio";
        }

        // Verificar si el correo ya está registrado (excepto si es el mismo correo)
        if (!coordinador.getCorreo().equals(correo) && coordinadorRepository.existsByCorreo(correo)) {
            redirectAttributes.addFlashAttribute("error", "Este correo ya está registrado.");
            return "redirect:/administrador/inicio"; 
        }

        // Actualizar los datos básicos del coordinador
        coordinador.setNombre(nombre);
        coordinador.setApellidos(apellidos);
        coordinador.setUsuario(usuario);

        // Solo actualizar la contraseña si se proporciona una nueva
        if (password != null && !password.isEmpty()) {
            coordinador.setPassword(passwordEncoder.encode(password)); 
        }

        coordinador.setCorreo(correo);
        coordinador.setTelefono(telefono);

        // Procesar los clubes seleccionados
        Set<Club> clubesAsignados = new HashSet<>();
        Set<String> clubesNoAsignados = new HashSet<>(); 

        if (clubes != null) {
            for (String nombreClub : clubes) {
                // Eliminamos acentos, espacios en blanco y convertimos a letras minúsculas
                String claveNombreClub = obtenerClave(nombreClub);
            
                // Buscamos el club 
                Club clubExistente = clubRepository.findByClave(claveNombreClub);

                // Comparamos los valores 
                if (clubExistente != null) {
                    clubExistente.setCoordinadorClub(coordinador);
                    clubesAsignados.add(clubExistente);
                } else {
                    clubesNoAsignados.add(nombreClub);
                }
            }
        }

        // Asignamos los clubes al coordinador
        coordinador.setClubesAsignados(clubesAsignados);

        // Guardar el coordinador en la base de datos
        coordinadorRepository.save(coordinador);

        // Guardar los clubes actualizados en la base de datos 
        clubRepository.saveAll(clubesAsignados);

        // Enviar los mensajes adecuados a la vista
        if (clubesNoAsignados.isEmpty()) {
            redirectAttributes.addFlashAttribute("success", "Coordinador actualizado con éxito. Todos los clubes fueron asignados.");
        } else {
            redirectAttributes.addFlashAttribute("warning", "Coordinador actualizado con éxito, pero los siguientes clubes no pudieron ser asignados: " 
            + String.join(", ", clubesNoAsignados));
        }

        // Redirigir al inicio del administrador
        return "redirect:/administrador/inicio"; 
    }

    // Método POST para eliminar un club
    @PostMapping("/eliminar-club")
    public String eliminarClub(@RequestParam Integer id, RedirectAttributes redirectAttributes) {

        // Buscar club por su ID
        Club club = clubRepository.findById(id).orElse(null);

        // Si no existe el club, mostrar un mensaje de error
        if (club == null) {
            redirectAttributes.addFlashAttribute("error", "Club no encontrado.");
            return "redirect:/administrador/inicio"; 
        }

        // Eliminar club de la base de datos
        clubRepository.delete(club);

        // Mensaje de éxito
        redirectAttributes.addFlashAttribute("success", "Club eliminado correctamente.");

        // Redirigir al inicio del administrador
        return "redirect:/administrador/inicio"; 
    }

    // Método POST para eliminar un coordinador
    @PostMapping("/eliminar-coordinador")
    public String eliminarCoordinador(@RequestParam Integer id, RedirectAttributes redirectAttributes) {

        // Buscar coordinador por su ID
        Coordinador coordinador = coordinadorRepository.findById(id).orElse(null);

        // Si no existe el coordinador, mostrar un mensaje de error
        if (coordinador == null) {
            redirectAttributes.addFlashAttribute("error", "Coordinador no encontrado.");
            return "redirect:/administrador/inicio"; 
        }

        // Eliminar coordinador de la base de datos
        coordinadorRepository.delete(coordinador);

        // Mensaje de éxito
        redirectAttributes.addFlashAttribute("success", "Coordinador eliminado correctamente.");

        // Redirigir al inicio del administrador
        return "redirect:/administrador/inicio"; 
    }

    // Método POST para eliminar un alumno
    @PostMapping("/eliminar-alumno")
    public String eliminarAlumno(@RequestParam Integer id, RedirectAttributes redirectAttributes) {

        // Buscar alumno por su ID
        Alumno alumno = alumnoRepository.findById(id).orElse(null);

        // Si no existe el alumno, mostrar un mensaje de error
        if (alumno == null) {
            redirectAttributes.addFlashAttribute("error", "Alumno no encontrado.");
            return "redirect:/administrador/inicio"; 
        }

        // Eliminar alumno de la base de datos
        alumnoRepository.delete(alumno);

        // Mensaje de éxito
        redirectAttributes.addFlashAttribute("success", "Alumno eliminado correctamente.");

        // Redirigir al inicio del administrador
        return "redirect:/administrador/inicio"; 
    }

    // Método para obtener la clave del Club
    private String obtenerClave(String nombre) {

        // Normalizamos la cadena para remover acentos
        String clave = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");

        // Eliminar espacios en blanco
        clave = clave.replaceAll("\\s+", "");

        // Convertir a letras minúsculas
        clave = clave.toLowerCase();

        return clave;
    }

    // Método para obtener las letras iniciales del Club
    private String obtenerInicialesClub(String nombre) {

        // Si el nombre del club es nulo o está vacío, devuelve una cadena vacía
        if (nombre == null || nombre.isBlank()) {
            return "";
        }

        // Divide el nombre del club en palabras, convirtiéndolo a mayúsculas y eliminando espacios extra
        String[] palabras = nombre.trim().toUpperCase().split("\\s+");

        // Si solo hay una palabra devuelve la primera letra
        if (palabras.length == 1) {
            return palabras[0].substring(0, 1);
        } else {
            // Si hay más de una palabra, devuelve las primeras letras de las dos primeras palabras
            return palabras[0].substring(0, 1) + palabras[1].substring(0, 1);
        }
    }
}
