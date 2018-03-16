package net.osomahe.cc.entity;

/**
 * TODO write JavaDoc
 *
 * @author Antonin Stoklasek
 */
public abstract class CBAggregate {

    protected String id;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "CBAggregate{" +
                "id='" + id + '\'' +
                '}';
    }
}
