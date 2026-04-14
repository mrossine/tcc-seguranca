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
    
    @Enumerated(EnumType.STRING)
    private StatusCarona status = StatusCarona.ABERTA;
    
    @CreatedDate
    private LocalDateTime dataCriacao;
    
    @OneToMany(mappedBy = "carona", cascade = CascadeType.ALL)
    private List<ParticipacaoCarona> participacoes;
    
    public enum StatusCarona {
        ABERTA, EM_ANDAMENTO, FINALIZADA, CANCELADA
    }
} 
