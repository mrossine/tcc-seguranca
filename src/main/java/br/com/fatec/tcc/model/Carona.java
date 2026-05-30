package br.com.fatec.tcc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "caronas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Carona {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "motorista_id", nullable = false)
    private Usuario motorista;
    
    @Column(nullable = false)
    private String origem;
    
    @Column(nullable = false)
    private String destino;
    
    @Column(nullable = false)
    private LocalDateTime horarioSaida;
    
    @Column(nullable = false)
    private Integer vagasDisponiveis;
    
    private String veiculoModelo;
    private String veiculoPlaca;
    
    @Column(length = 500)
    private String observacoes;

    /** Coordenadas do destino usadas pelo geofencing (raio de 50 m). */
    private Double destinoLatitude;
    private Double destinoLongitude;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)")
    private StatusCarona status = StatusCarona.ABERTA;
    
    @CreatedDate
    private LocalDateTime dataCriacao;
    
    @OneToMany(mappedBy = "carona", cascade = CascadeType.ALL)
    private List<ParticipacaoCarona> participacoes;
    
    public enum StatusCarona {
        ABERTA,     // tem vagas em aberto — visível para todos
        CHEIA,      // todas as vagas preenchidas — visível só para motorista e passageiros confirmados
        FECHADA,    // horário de saída passou — rastreamento em tempo real ativo
        COMPLETADA, // motorista chegou ao destino — aguardando 10 min para FINALIZADA
        FINALIZADA, // carona encerrada — disponível para avaliação
        CANCELADA   // motorista cancelou antes do início
    }
} 
