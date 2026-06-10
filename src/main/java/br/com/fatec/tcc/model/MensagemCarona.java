package br.com.fatec.tcc.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Mensagem de texto trocada entre o motorista e os passageiros de uma carona.
 * Apenas participantes confirmados (motorista ou passageiro CONFIRMADO) podem
 * enviar e visualizar mensagens.
 */
@Entity
@Table(name = "mensagens_carona",
       indexes = @Index(name = "idx_mensagem_carona_data", columnList = "carona_id, data_criacao"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MensagemCarona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "carona_id", nullable = false)
    private Carona carona;

    @ManyToOne
    @JoinColumn(name = "remetente_id", nullable = false)
    private Usuario remetente;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    @CreatedDate
    private LocalDateTime dataCriacao;

    /** Indica se o destinatário já leu a mensagem. */
    private Boolean lido = false;

    /** Mensagem gerada automaticamente pelo sistema (ex.: "Fulano entrou na carona"). */
    private Boolean sistema = false;
}
