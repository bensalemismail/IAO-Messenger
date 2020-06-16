package world.iaomessenger;

public class ContactsModule {

    public String status;
    public String name;
    public String image;
    public String uid;
    public String online;

    ContactsModule() {}

    public ContactsModule(String status, String name, String image, String uid, String online) {
        this.status = status;
        this.name = name;
        this.image = image;
        this.uid = uid;
        this.online = online;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getOnline() {
        return online;
    }

    public void setOnline(String online) {
        this.online = online;
    }
}
