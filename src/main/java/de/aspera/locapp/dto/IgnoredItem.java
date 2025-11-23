package de.aspera.locapp.dto;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;


@Entity
@Table(name = "IGNORED_ITEM", uniqueConstraints = @UniqueConstraint(columnNames = { "fileName"}))
public class IgnoredItem implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = -4226633978528077781L;

	@Id
    @GeneratedValue
    private String id;

    private String fileName;
    
    @Column(name = "ID")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Column(name = "FILENAME")
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public int hashCode() {
        if (id == null) {
            return "".hashCode();
        }

        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof IgnoredItem)) {
            return false;
        }

        if (id == null) {
            return false;
        }

        return id.equals(((IgnoredItem)obj).id);
    }
}
