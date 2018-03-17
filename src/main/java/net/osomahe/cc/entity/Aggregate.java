package net.osomahe.cc.entity;

/**
 * TODO write JavaDoc
 *
 * @author Antonin Stoklasek
 */
public abstract class Aggregate {

    protected String id;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Aggregate{" +
                "id='" + id + '\'' +
                '}';
    }
}
