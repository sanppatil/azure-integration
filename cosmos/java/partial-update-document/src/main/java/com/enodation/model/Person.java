package com.enodation.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class Person {

    @JsonProperty("id")
    private String id;

    @JsonProperty("partition_key")
    private String partition_key;

    @JsonProperty("DocumentType")
    private String documentType;
    @JsonProperty("FirstName")
    private String firstName;

    @JsonProperty("LastName")
    private String lastName;

    @JsonProperty("MailingAddress")
    private Address mailingAddress;

    public static Person createPerson()
    {
        Person person = new Person();
        person.id = "1234_Person";
        person.partition_key = "1234";
        person.documentType = "Person";
        person.firstName = "John";
        person.lastName = "Smith";
        person.mailingAddress = new Address();
        person.mailingAddress.setAddressLine1("2220 NJ-27");
        person.mailingAddress.setCity("Edison");
        person. mailingAddress.setState("NJ");
        person.mailingAddress.setZipCode("08817");
        person.mailingAddress.setCountry("USA");
        return person;
    }
}
