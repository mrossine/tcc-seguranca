package br.com.fatec.tcc.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Denúncia de um ALERTA de segurança (conteúdo falso, ofensivo, spam, etc.).
 *
 * Diferente da denúncia de carona, esta sempre é feita por um usuário contra um
 * alerta específico, acompanhada de uma justificativa em texto livre. Todas ficam
 * visíveis para o administrador analisar, numa categoria separada das de carona.
 */
@Entity
@Table(name = "denuncias_alerta",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_denuncia_alerta_usuario",
               columnNames = {"alerta_id", "denunciante_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DenunciaAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Alerta que está sendo denunciado */
    @ManyToOne
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

    /** Usuário que registrou a denúncia */
    @ManyToOne
    @JoinColumn(name = "denunciante_id", nullable = false)
    private Usuario denunciante;

    /** Motivo da denúncia */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(30)")
    private CategoriaDenuncia categoria;

    /** Justificativa escrita pelo denunciante */
    @Column(nullable = false, length = 1000)
    private String justificativa;

    /** Situação da análise pelo administrador */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private StatusDenuncia status = StatusDenuncia.PENDENTE;

    @CreatedDate
    private LocalDateTime dataDenuncia;

    public enum CategoriaDenuncia {
        CONTEUDO_FALSO,
        CONTEUDO_OFENSIVO,
        SPAM,
        LOCAL_INCORRETO,
        OUTRO
    }

    public enum StatusDenuncia {
        PENDENTE,    // ainda não analisada
        EM_ANALISE,  // administrador está avaliando
        RESOLVIDA,   // providência tomada
        ARQUIVADA    // sem providência / improcedente
    }
}
