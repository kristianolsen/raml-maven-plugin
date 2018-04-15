package no.trinnvis;

interface Visitable<T> {

    void accept(Visitor<T> visitor);
}
