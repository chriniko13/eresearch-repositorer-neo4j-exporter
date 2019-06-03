package com.chriniko.eresearchreponeo4jexporter.domain.mongo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AuthorTest {

    @Test
    public void fullname_no_initials_case() {

        // given
        Author author = new Author();
        author.setFirstname("Firstname");
        author.setSurname("Surname");

        // when
        String fullname = Author.fullname(author);


        // then
        assertEquals("Firstname Surname", fullname);
    }

    @Test
    public void fullname_initials_case() {

        // given
        Author author = new Author();
        author.setFirstname("Firstname");
        author.setInitials("Initials");
        author.setSurname("Surname");

        // when
        String fullname = Author.fullname(author);


        // then
        assertEquals("Firstname Initials Surname", fullname);
    }

    @Test
    public void fullname_no_firstname_case() {

        // given
        Author author = new Author();
        author.setInitials("Initials");
        author.setSurname("Surname");

        // when
        String fullname = Author.fullname(author);


        // then
        assertEquals("Initials Surname", fullname);
    }

    @Test
    public void only_surname_case() {

        // given
        Author author = new Author();
        author.setSurname("Surname");

        // when
        String fullname = Author.fullname(author);


        // then
        assertEquals("Surname", fullname);
    }

}