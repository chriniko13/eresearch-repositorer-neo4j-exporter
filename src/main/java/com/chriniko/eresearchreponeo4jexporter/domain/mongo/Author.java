package com.chriniko.eresearchreponeo4jexporter.domain.mongo;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Author implements Serializable {

    private String firstname;
    private String initials;
    private String surname;

    public static String fullname(com.chriniko.eresearchreponeo4jexporter.domain.mongo.Author author) {

        StringBuilder sb = new StringBuilder();

        String firstname = author.getFirstname();
        String initials = author.getInitials();
        String surname = author.getSurname();

        if (firstname != null && !firstname.isEmpty()) {
            sb.append(firstname);
        }

        if (initials != null && !initials.isEmpty()) {
            if (sb.toString().isEmpty()) {
                sb.append(initials);
            } else {
                sb.append(" ").append(initials);
            }
        }

        if (surname != null && !surname.isEmpty()) {
            if (sb.toString().isEmpty()) {
                sb.append(surname);
            } else {
                sb.append(" ").append(surname);
            }
        }

        return sb.toString();
    }
}
