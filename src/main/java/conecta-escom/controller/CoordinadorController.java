package conecta_escom.controller;

import conecta_escom.model.Club;
import conecta_escom.model.Coordinador;
import conecta_escom.repository.ClubRepository;
import conecta_escom.repository.CoordinadorRepository;
import conecta_escom.service.FileUploadService;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.text.Normalizer;
import java.util.List;

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
@RequestMapping("/coordinador")
public class CoordinadorController {

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private CoordinadorRepository coordinadorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileUploadService fileUploadService;

    // Página principal del Coordinador
    @GetMapping("/inicio")
    public String inicioAdministrador(HttpSession session, Model model) {

        // Recuperamos el usuario y rol de la sesión
        Object usuario = session.getAttribute("usuario");
        String rol = (String) session.getAttribute("rol");

        // Verificamos si la sesión está activa y si el usuario es un coordinador
        if (usuario == null || !"COORDINADOR".equals(rol)) {
            return "redirect:/login"; 
        }

        // Convierte el objeto usuario a Coordinador
        Coordinador coordinador = (Coordinador) usuario;

        // Lógica de la página de inicio para el administrador
        List<Club> clubes = clubRepository.findByCoordinadorClub(coordinador);;
        long cantidadClubes = clubes.size();

        // Agregamos los datos al modelo
        model.addAttribute("coordinador", coordinador);
        model.addAttribute("clubes", clubes);
        model.addAttribute("cantidadClubes", cantidadClubes);
       
        // Retornamos la vista para la página de inicio del coordinador       
        return "coordinador-inicio";
    }

    // Método GET que devuelve la página para editar el perfil del Coordinador
    @GetMapping("/editar-perfil")
    public String coordinadorEditarPerfil(HttpSession session, Model model) {

        // Recuperamos el usuario y rol de la sesión
        Object usuario = session.getAttribute("usuario");
        String rol = (String) session.getAttribute("rol");

        // Verificamos si la sesión está activa y si el usuario es un Coordinador
        if (usuario == null || !"COORDINADOR".equals(rol)) {
            return "redirect:/login";
        }

        // Convierte el objeto usuario a Coordinador
        Coordinador coordinador = (Coordinador) usuario;

        model.addAttribute("coordinador", coordinador);
           
        return "coordinador-editar-perfil";
    }

    //Método POST para actualizar la foto de perfil del Coordinador
    @PostMapping("/editar-perfil/foto")
    public String coordinadorActualizarFotoPerfil(
        @RequestParam("id") Integer id, 
        @RequestParam("fotoCoordinador") MultipartFile fotoCoordinador,
        HttpSession session,
        RedirectAttributes redirectAttributes) {

        // Buscar Coordinador por su ID
        Coordinador coordinador = coordinadorRepository.findById(id).orElse(null);

        // Si el Coordinador no existe, mostrar un error
        if (coordinador == null) {
            redirectAttributes.addFlashAttribute("error", "Coordinador no encontrado.");
            return "redirect:/coordinador/editar-perfil"; 
        }

        // Actualizar Foto de Perfil del Coordinador
        if (fotoCoordinador != null && !fotoCoordinador.isEmpty()) {
            try {
                String nombreArchivo = "Foto Perfil - " + coordinador.getUsuario();
                String rutaFotoPerfil = fileUploadService.guardarArchivo(fotoCoordinador, "coordinadores", coordinador.getUsuario(), nombreArchivo);
                coordinador.setRutaFotoPerfil(rutaFotoPerfil);
            } catch (IOException | InvalidPathException e) {
                redirectAttributes.addFlashAttribute("error", "Error al guardar la imagen: " + e.getMessage());
                 return "redirect:/coordinador/editar-perfil";
            }
        }

        // Guardar los cambios del Coordinador en la base de datos
        coordinadorRepository.save(coordinador);

        // Actualizar Sesión
        session.setAttribute("usuario", coordinador);

        // Mensaje de éxito
        redirectAttributes.addFlashAttribute("success", "Foto de Perfil actualizada");

        return "redirect:/coordinador/editar-perfil"; 
    }

    //Método POST para editar los datos del Coordinador
    @PostMapping("/editar-perfil/datos")
    public String coordinadorActualizarDatos(
        @RequestParam Integer id,
        @RequestParam String nombre,
        @RequestParam String apellidos,
        @RequestParam String correo,
        @RequestParam String telefono,
        @RequestParam String password,
        HttpSession session,
        RedirectAttributes redirectAttributes) {

        // Buscar Coordinador por su ID
        Coordinador coordinador = coordinadorRepository.findById(id).orElse(null);

        // Si el Coordinador no existe, mostrar un error
        if (coordinador == null) {
            redirectAttributes.addFlashAttribute("error", "Coordinador no encontrado.");
            return "redirect:/coordinador/editar-perfil"; 
        }

        // Actualizar los campos del Coordinador con los nuevos datos
        coordinador.setNombre(nombre);
        coordinador.setApellidos(apellidos);
        coordinador.setCorreo(correo);
        coordinador.setTelefono(telefono);
               
        // Solo actualizar la contraseña si se proporciona una nueva
        if (password != null && !password.isEmpty()) {
            coordinador.setPassword(passwordEncoder.encode(password)); 
        }

        // Guardar los cambios del Coordinador en la base de datos
        coordinadorRepository.save(coordinador);

        // Actualizar Sesión
        session.setAttribute("usuario", coordinador);

        // Mensaje de éxito
        redirectAttributes.addFlashAttribute("success", "Datos actualizados correctamente.");

         return "redirect:/coordinador/editar-perfil"; 
    }

    // Método GET que devuelve la página para editar los detalles del Club
    @GetMapping("/editar-club/{id}")
    public String detalleClub(
        @PathVariable Integer id,
        HttpSession session,
        Model model) {

        Object usuario = session.getAttribute("usuario");
        String rol = (String) session.getAttribute("rol");

        if (usuario == null || !"COORDINADOR".equals(rol)) {
            return "redirect:/login";
        }

        Coordinador coordinador = (Coordinador) usuario;

        Club club = clubRepository.findById(id).orElse(null);
        if (club == null) {
            return "redirect:/alumno/inicio";
        }

        model.addAttribute("coordinador", coordinador);
        model.addAttribute("club", club);
    
        return "coordinador-editar-club";
    }

    //Método POST para actualizar la foto del Club
    @PostMapping("/editar-club/foto")
    public String actualizarFotoClub(
        @RequestParam("id") Integer id, 
        @RequestParam("imagenClub") MultipartFile imagenClub,
        RedirectAttributes redirectAttributes) {

        // Buscar club por su ID
        Club club = clubRepository.findById(id).orElse(null);

        // Si el club no existe, mostrar un error
        if (club == null) {
            redirectAttributes.addFlashAttribute("error", "Club no encontrado.");
            return "redirect:/coordinador/editar-club/" + id; 
        }

        // Actualizar Imagen del Club
        if (imagenClub != null && !imagenClub.isEmpty()) {
            try {
                String nombreArchivo = "Imagen Club - " + club.getNombre();
                String rutaArchivoImagen = fileUploadService.guardarArchivo(imagenClub, "clubes", club.getNombre(), nombreArchivo);
                club.setRutaArchivoImagen(rutaArchivoImagen);
            } catch (IOException | InvalidPathException e) {
                redirectAttributes.addFlashAttribute("error", "Error al guardar la imagen: " + e.getMessage());
                 return "redirect:/coordinador/editar-club/" + id;
            }
        }

        // Guardar los cambios del club en la base de datos
        clubRepository.save(club);

        // Mensaje de éxito
        redirectAttributes.addFlashAttribute("success", "Imagen del Club actualizada");

         return "redirect:/coordinador/editar-club/" + id; 
    }

    //Método POST para editar la información de un club
    @PostMapping("/editar-club/datos")
    public String editarClub(
        @RequestParam Integer id,
        @RequestParam String nombre,
        @RequestParam String tipo,
        @RequestParam String objetivo,
        @RequestParam String ubicacion,
        @RequestParam String horario,
        @RequestParam(required = false) MultipartFile archivoCronograma,
        RedirectAttributes redirectAttributes) {

        // Buscar club por su ID
        Club club = clubRepository.findById(id).orElse(null);

        // Si el club no existe, mostrar un error
        if (club == null) {
            redirectAttributes.addFlashAttribute("error", "Club no encontrado.");
             return "redirect:/coordinador/editar-club/" + id; 
        }

        // Actualizar los campos del club con los nuevos datos
        club.setNombre(nombre);
        club.setClave(obtenerClave(nombre));
        club.setInicialesClub(obtenerInicialesClub(nombre));
        club.setTipo(tipo);
        club.setObjetivo(objetivo);
        club.setUbicacion(ubicacion);
        club.setHorario(horario);

        // Actualizar el cronograma si es que se ha subido un nuevo archivo
        if (archivoCronograma != null && !archivoCronograma.isEmpty()) {
            try {
                String nombreArchivo = "Cronograma - " + nombre;
                String rutaArchivoCronograma = fileUploadService.guardarArchivo(archivoCronograma, "clubes", nombre, nombreArchivo);
                club.setRutaArchivoCronograma(rutaArchivoCronograma);
            } catch (IOException | InvalidPathException e) {
                redirectAttributes.addFlashAttribute("error", "Error al guardar el cronograma: " + e.getMessage());
                 return "redirect:/coordinador/editar-club/" + id;
            }
        }

        // Guardar los cambios del club en la base de datos
        clubRepository.save(club);

        // Mensaje de éxito
        redirectAttributes.addFlashAttribute("success", "Club actualizado correctamente.");

         return "redirect:/coordinador/editar-club/" + id; 
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
