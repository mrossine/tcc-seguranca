package br.com.fatec.tcc.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Denúncia de ocorrido numa carona.
 *
 * Pode ser feita:
 *  - pelo PASSAGEIRO (denuncia o motorista da carona);
 *  - pelo MOTORISTA (denuncia um passageiro específico — ou a carona inteira,
 *    o que gera uma denúncia para cada passageiro confirmado).
 *
 * Só é permitida após a carona estar FINALIZADA. Todas as denúncias ficam
 * visíveis para o administrador analisar.
 */
@Entity
@Table(name = "denuncias_carona",
       uniqueConstraints = @UniqueConstraint(
               columnNames = {"carona_id", "denunciante_id", "denunciado_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DenunciaCarona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Carona em que ocorreu o fato denunciado */
    @ManyToOne
    @JoinColumn(name = "carona_id", nullable = false)
    private Carona carona;

    /** Quem registrou a denúncia */
    @ManyToOne
    @JoinColumn(name = "denunciante_id", nullable = false)
    private Usuario denunciante;

    /** Quem está sendo denunciado (motorista ou passageiro) */
    @ManyToOne
    @JoinColumn(name = "denunciado_id", nullable = false)
    private Usuario denunciado;

    /** Papel de quem denunciou (PASSAGEIRO ou MOTORISTA) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private TipoDenunciante tipoDenunciante;

    /** Categoria do ocorrido */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(30)")
    private CategoriaDenuncia categoria;

    /** Descrição detalhada do ocorrido */
    @Column(nullable = false, length = 1000)
    private String descricao;

    /** Situação da análise pelo administrador */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private StatusDenuncia status = StatusDenuncia.PENDENTE;

    @CreatedDate
    private LocalDateTime dataDenuncia;

    public enum TipoDenunciante {
        PASSAGEIRO, MOTORISTA
    }

    public enum CategoriaDenuncia {
        COMPORTAMENTO_INADEQUADO,
        DIRECAO_PERIGOSA,
        ASSEDIO,
        COBRANCA_INDEVIDA,
        ATRASO_OU_AUSENCIA,
        VEICULO_INADEQUADO,
        OUTRO
    }

    public enum StatusDenuncia {
        PENDENTE,    // ainda não analisada
        EM_ANALISE,  // administrador está avaliando
        RESOLVIDA,   // providência tomada
        ARQUIVADA    // sem providência / improcedente
    }
}
