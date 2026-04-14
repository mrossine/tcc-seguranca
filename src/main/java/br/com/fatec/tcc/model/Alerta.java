package br.com.fatec.tcc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "alertas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Alerta {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String titulo;
    
    @Column(length = 1000)
    private String descricao;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAlerta tipo;
    
    @Column(nullable = false)
    private String localizacao;
    
    private Double latitude;
    private Double longitude;
    
    @Column(nullable = false)
    private LocalDateTime dataHora;
    
    @Enumerated(EnumType.STRING)
    private StatusAlerta status = StatusAlerta.ATIVO;
    
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @CreatedDate
    private LocalDateTime dataCriacao;
    
    private Integer confirmacoes = 0;
    private Integer denuncias = 0;
    
    public enum TipoAlerta {
        FURTO("Furto"),
        ROUBO("Roubo"),
        ATIVIDADE_SUSPEITA("Atividade Suspeita"),
        FALTA_ILUMINACAO("Falta de Iluminação"),
        OUTRO("Outro");
        
        private String descricao;
        
        TipoAlerta(String descricao) {
            this.descricao = descricao;
        }
        
        public String getDescricao() {
            return descricao;
        }
    }
    
    public enum StatusAlerta {
        ATIVO, INATIVO, DENUNCIADO
    }
} 
