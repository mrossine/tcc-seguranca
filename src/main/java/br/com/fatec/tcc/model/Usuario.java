package br.com.fatec.tcc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Usuario implements UserDetails {
    
	private static final long serialVersionUID = 1L;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String nomeCompleto;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String senha;
    
    @Column(unique = true, nullable = false)
    private String matricula;
    
    private String curso;
    
    @Enumerated(EnumType.STRING)
    private Periodo periodo;
    
    private String fotoPerfil;
    
    @CreatedDate
    private LocalDateTime dataCadastro;
    
    private Boolean ativo = true;
    
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;
    
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Alerta> alertas;
    
    @OneToMany(mappedBy = "motorista")
    private List<Carona> caronasOferecidas;
    
    @OneToMany(mappedBy = "passageiro")
    private List<ParticipacaoCarona> participacoes;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    @Override
    public String getPassword() {
        return senha;
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return ativo;
    }
    
    public enum Periodo {
        MANHA, TARDE, NOITE
    }
    
    public enum Role {
        USER, MODERATOR, ADMIN
    }
} 
