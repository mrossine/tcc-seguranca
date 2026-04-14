package br.com.fatec.tcc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "participacoes_carona")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ParticipacaoCarona {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "carona_id", nullable = false)
    private Carona carona;
    
    @ManyToOne
    @JoinColumn(name = "passageiro_id", nullable = false)
    private Usuario passageiro;
    
    @Enumerated(EnumType.STRING)
    private StatusParticipacao status = StatusParticipacao.SOLICITADA;
    
    @CreatedDate
    private LocalDateTime dataSolicitacao;
    
    private LocalDateTime dataConfirmacao;
    
    public enum StatusParticipacao {
        SOLICITADA, CONFIRMADA, RECUSADA, CANCELADA
    }
} 
