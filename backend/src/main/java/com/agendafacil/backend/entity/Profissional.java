package com.agendafacil.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "profissional")
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false)
    private boolean ativo;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "profissional_servico",
            joinColumns = @JoinColumn(name = "profissional_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id"))
    private Set<Servico> servicos = new HashSet<>();

    @OneToMany(mappedBy = "profissional", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Jornada> jornada = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Set<Servico> getServicos() {
        return servicos;
    }

    public void setServicos(Set<Servico> servicos) {
        this.servicos = servicos;
    }

    public List<Jornada> getJornada() {
        return jornada;
    }

    public void setJornada(List<Jornada> jornada) {
        this.jornada = jornada;
    }
}
