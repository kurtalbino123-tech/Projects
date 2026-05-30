// ADDED (ENTIRE FILE - NEW): Model class to represent an address with properties for table binding
public class Address {
    private int requestId; // Database ID for the document request
    // ADDED LINE 3: residentName field for storing resident name
    private String residentName;
    private String address;
    private String city;
    private String zipCode;
    private String status;
    private String reason;

    // Constructor to initialize all address fields
    public Address(String residentName, String address, String city, String zipCode) {
        this.residentName = residentName;
        this.address = address;
        this.city = city;
        this.zipCode = zipCode;
    }

    // Overloaded constructor for Document Requests
    public Address(int id, String residentName, String documentType, String status, String reason) {
        this.requestId = id;
        this.residentName = residentName;
        this.address = documentType; // Reusing address field for document type
        this.status = status;
        this.reason = reason;
    }

    //Getter methods (required for PropertyValueFactory to work with TableView)
    public String getResidentName() {
        return residentName;
    }

    public int getRequestId() {
        return requestId;
    }

    public String getAddress() {
        return address;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getCity() {
        return city;
    }

    public String getZipCode() {
        return zipCode;
    }

    // Setter methods for updating address properties
    public void setResidentName(String residentName) {
        this.residentName = residentName;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
}
