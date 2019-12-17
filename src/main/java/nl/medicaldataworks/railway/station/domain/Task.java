package nl.medicaldataworks.railway.station.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import nl.medicaldataworks.railway.station.domain.CalculationStatus;
import nl.medicaldataworks.railway.station.domain.Train;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
public class Task {
    @Id
    @GeneratedValue
    private Long id;
    @CreationTimestamp
    private Date creationTimestamp;
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JsonBackReference
    private Train train;
    private CalculationStatus calculationStatus;
    private String result;
    private String clientId;
    private String ownerName;
}