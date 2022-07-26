package com.ivan.andatest.models;

public class Driver {

    String id;
    String name;
    String email;
    String vehicleBrand;
    String plate;

    public Driver(String id, String name, String email, String vehicleBrand, String plate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.vehicleBrand = vehicleBrand;
        this.plate = plate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVehicleBrand() {
        return vehicleBrand;
    }

    public void setVehicleBrand(String vehicleBrand) {
        this.vehicleBrand = vehicleBrand;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }
}
