package conecta_escom.repository;

import conecta_escom.model.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministradorRepository extends JpaRepository<Administrador, Integer> {
    Administrador findByUsuario(String usuario);
}
