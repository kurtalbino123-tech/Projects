
public class AddResidentDialogController {
    private String name;
    private String contact;
    private String address;
    private String classification;

    // Constructor
    public AddResidentDialogController(String name, String contact, String address, String classification) {
        this.name = name;
        this.contact = contact;
        this.address = address;
        this.classification = classification;
    }

    // Getters (required for PropertyValueFactory to work)
    public String getName() {
        return name;
    }

    public String getContact() {
        return contact;
    }

    public String getAddress() {
        return address;
    }

    public String getClassification() {
        return classification;
    }

    // Setters 
    public void setName(String name) {
        this.name = name;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }
}
