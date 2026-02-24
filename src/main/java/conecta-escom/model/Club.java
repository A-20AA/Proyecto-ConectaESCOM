package conecta_escom.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clubes")
@NoArgsConstructor
@Getter
@Setter
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String clave;

    @Column(nullable = false)
    private String inicialesClub;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false, length = 1000)
    private String objetivo;

    @Column(nullable = false)
    private String ubicacion;

    @Column(nullable = false)
    private String horario;

    @ManyToOne
    private Coordinador coordinadorClub;

    @ManyToOne
    private Alumno presidenteClub;

    @Column
    private String rutaArchivoCronograma;

    @Column
    private String rutaArchivoImagen;

    @ManyToMany(mappedBy = "clubesInscritos")
    private Set<Alumno> alumnosInscritos = new HashSet<>();

    public int getCantidadAlumnosInscritos() {
        return alumnosInscritos.size();
    }
}
