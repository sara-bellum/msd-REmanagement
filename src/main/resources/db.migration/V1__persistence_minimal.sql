-- PostgreSQL schema for Sprint 3. Do not edit after applying; add V2, V3, ...
-- Technical primary keys and root domain IDs use separate sequences.
CREATE SEQUENCE entity_pk_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE domain_id_seq START WITH 1 INCREMENT BY 1;

-- Shared person identity and contact fields.
CREATE TABLE persoane (
                          persistence_id bigint PRIMARY KEY,
                          domain_id integer NOT NULL CHECK (domain_id > 0),
                          version bigint,
                          created_at timestamptz NOT NULL,
                          updated_at timestamptz NOT NULL,
                          nume varchar(255),
                          prenume varchar(255),
                          telefon varchar(255),
                          email varchar(255),
                          adresa varchar(255),
                          tip_persoana varchar(255),
                          UNIQUE (domain_id)
);

-- JOINED subclass: reuse the person primary key for agent-specific fields.
CREATE TABLE agenti (
                        persistence_id bigint PRIMARY KEY REFERENCES persoane(persistence_id),
                        data_angajare date,
                        salariu_baza numeric(19,2)
);

-- Shared geographical reference data.
CREATE TABLE zone (
                      persistence_id bigint PRIMARY KEY,
                      domain_id integer NOT NULL CHECK (domain_id > 0),
                      version bigint,
                      created_at timestamptz NOT NULL,
                      updated_at timestamptz NOT NULL,
                      nume_zona varchar(255),
                      localitate varchar(255),
                      judet varchar(255),
                      UNIQUE (domain_id)
);

-- Agent-owned zone assignments.
CREATE TABLE agent_zone (
                            persistence_id bigint PRIMARY KEY,
                            domain_id integer NOT NULL CHECK (domain_id > 0),
                            version bigint,
                            created_at timestamptz NOT NULL,
                            updated_at timestamptz NOT NULL,
                            zona_id bigint NOT NULL,
                            data_inceput date,
                            data_sfarsit date,
                            zona_activa boolean NOT NULL,
                            agent_id bigint REFERENCES agenti(persistence_id),
                            UNIQUE (agent_id, domain_id)
);

-- Property data references an owner and a zone without owning their lifecycle.
CREATE TABLE proprietati (
                             persistence_id bigint PRIMARY KEY,
                             domain_id integer NOT NULL CHECK (domain_id > 0),
                             version bigint,
                             created_at timestamptz NOT NULL,
                             updated_at timestamptz NOT NULL,
                             proprietar_id bigint NOT NULL,
                             zona_id bigint NOT NULL,
                             tip_proprietate varchar(255),
                             adresa varchar(255),
                             suprafata numeric(19,2),
                             numar_camere integer NOT NULL,
                             etaj integer NOT NULL,
                             facilitati varchar(255),
                             descriere varchar(255),
                             UNIQUE (domain_id),
                             CHECK (suprafata > 0),
                             CHECK (numar_camere >= 0)
);

-- Listing aggregate root; reservation fields link it to a transaction result.
CREATE TABLE oferte (
                        persistence_id bigint PRIMARY KEY,
                        domain_id integer NOT NULL CHECK (domain_id > 0),
                        version bigint,
                        created_at timestamptz NOT NULL,
                        updated_at timestamptz NOT NULL,
                        proprietate_id bigint NOT NULL,
                        tip_oferta varchar(255),
                        agent_id bigint,
                        descriere_scurta varchar(255),
                        pret_listare numeric(19,2),
                        data_creare date,
                        stare varchar(255),
                        tranzactie_rezervare integer,
                        next_image integer NOT NULL,
                        UNIQUE (domain_id),
                        CHECK (pret_listare > 0)
);

-- Listing-owned image metadata.
CREATE TABLE imagini_oferte (
                                persistence_id bigint PRIMARY KEY,
                                domain_id integer NOT NULL CHECK (domain_id > 0),
                                version bigint,
                                created_at timestamptz NOT NULL,
                                updated_at timestamptz NOT NULL,
                                url varchar(255),
                                descriere varchar(255),
                                data_incarcare date,
                                ordine_afisare integer NOT NULL,
                                este_principala boolean NOT NULL,
                                oferta_id bigint REFERENCES oferte(persistence_id),
                                UNIQUE (oferta_id, domain_id)
);

-- Client request aggregate root.
CREATE TABLE cereri (
                        persistence_id bigint PRIMARY KEY,
                        domain_id integer NOT NULL CHECK (domain_id > 0),
                        version bigint,
                        created_at timestamptz NOT NULL,
                        updated_at timestamptz NOT NULL,
                        client_id bigint NOT NULL,
                        tip_cerere varchar(255),
                        tip_proprietate varchar(255),
                        buget_min numeric(19,2),
                        buget_max numeric(19,2),
                        suprafata_dorita numeric(19,2),
                        nr_camere_dorite integer NOT NULL,
                        data_cerere date,
                        stare varchar(255),
                        UNIQUE (domain_id),
                        CHECK (buget_min >= 0 AND buget_max > 0 AND buget_max >= buget_min)
);

-- Request-owned preferred zones.
CREATE TABLE cerere_zone (
                             persistence_id bigint PRIMARY KEY,
                             domain_id integer NOT NULL CHECK (domain_id > 0),
                             version bigint,
                             created_at timestamptz NOT NULL,
                             updated_at timestamptz NOT NULL,
                             zona_id bigint NOT NULL,
                             prioritate integer NOT NULL,
                             cerere_id bigint REFERENCES cereri(persistence_id),
                             UNIQUE (cerere_id, domain_id)
);

-- Client interest connecting a client, listing and optional request.
CREATE TABLE interese (
                          persistence_id bigint PRIMARY KEY,
                          domain_id integer NOT NULL CHECK (domain_id > 0),
                          version bigint,
                          created_at timestamptz NOT NULL,
                          updated_at timestamptz NOT NULL,
                          client_id bigint NOT NULL,
                          oferta_id bigint NOT NULL,
                          cerere_id bigint,
                          data_interes date,
                          observatii varchar(255),
                          stare varchar(255),
                          UNIQUE (domain_id)
);

-- Interest-owned viewings and feedback.
CREATE TABLE vizionari (
                           persistence_id bigint PRIMARY KEY,
                           domain_id integer NOT NULL CHECK (domain_id > 0),
                           version bigint,
                           created_at timestamptz NOT NULL,
                           updated_at timestamptz NOT NULL,
                           interes_id bigint NOT NULL,
                           agent_id bigint NOT NULL,
                           data_ora timestamp(6),
                           note varchar(255),
                           feedback varchar(255),
                           nivel_interes integer,
                           decizie varchar(255),
                           stare varchar(255),
                           UNIQUE (interes_id, domain_id)
);

-- Minimal transaction state: initiation and cancellation only.
CREATE TABLE rezultate (
                           persistence_id bigint PRIMARY KEY,
                           domain_id integer NOT NULL CHECK (domain_id > 0),
                           version bigint,
                           created_at timestamptz NOT NULL,
                           updated_at timestamptz NOT NULL,
                           interes_id bigint NOT NULL,
                           agent_responsabil_id bigint NOT NULL,
                           tip_rezultat varchar(255),
                           data_rezultat date,
                           data_finalizare date,
                           pret_convenit numeric(19,2),
                           comision_cumparator numeric(19,2),
                           comision_vanzator numeric(19,2),
                           comision_cumparator_platit boolean NOT NULL,
                           comision_vanzator_platit boolean NOT NULL,
                           motiv_anulare varchar(255),
                           stare varchar(255),
                           UNIQUE (domain_id)
);

-- Do not enable advanced transaction states before their data is fully mapped and saved.
ALTER TABLE rezultate ADD CONSTRAINT ck_rezultat_minimal CHECK (stare IN ('INITIAT', 'ANULAT'));

-- Transaction-owned agent participation records.
CREATE TABLE agenti_rezultate (
                                  persistence_id bigint PRIMARY KEY,
                                  domain_id integer NOT NULL CHECK (domain_id > 0),
                                  version bigint,
                                  created_at timestamptz NOT NULL,
                                  updated_at timestamptz NOT NULL,
                                  agent_id bigint NOT NULL,
                                  rezultat_id bigint NOT NULL,
                                  UNIQUE (rezultat_id, domain_id),
                                  UNIQUE (rezultat_id, agent_id)
);

ALTER TABLE agent_zone ADD FOREIGN KEY (zona_id) REFERENCES zone(persistence_id);
ALTER TABLE proprietati ADD FOREIGN KEY (proprietar_id) REFERENCES persoane(persistence_id);
ALTER TABLE proprietati ADD FOREIGN KEY (zona_id) REFERENCES zone(persistence_id);
ALTER TABLE oferte ADD FOREIGN KEY (proprietate_id) REFERENCES proprietati(persistence_id);
ALTER TABLE oferte ADD FOREIGN KEY (agent_id) REFERENCES agenti(persistence_id);
ALTER TABLE cereri ADD FOREIGN KEY (client_id) REFERENCES persoane(persistence_id);
ALTER TABLE cerere_zone ADD FOREIGN KEY (zona_id) REFERENCES zone(persistence_id);
ALTER TABLE interese ADD FOREIGN KEY (client_id) REFERENCES persoane(persistence_id);
ALTER TABLE interese ADD FOREIGN KEY (oferta_id) REFERENCES oferte(persistence_id);
ALTER TABLE interese ADD FOREIGN KEY (cerere_id) REFERENCES cereri(persistence_id);
ALTER TABLE vizionari ADD FOREIGN KEY (interes_id) REFERENCES interese(persistence_id);
ALTER TABLE vizionari ADD FOREIGN KEY (agent_id) REFERENCES agenti(persistence_id);
ALTER TABLE rezultate ADD FOREIGN KEY (interes_id) REFERENCES interese(persistence_id);
ALTER TABLE rezultate ADD FOREIGN KEY (agent_responsabil_id) REFERENCES agenti(persistence_id);
ALTER TABLE agenti_rezultate ADD FOREIGN KEY (agent_id) REFERENCES agenti(persistence_id);
ALTER TABLE agenti_rezultate ADD FOREIGN KEY (rezultat_id) REFERENCES rezultate(persistence_id);

ALTER TABLE oferte ADD FOREIGN KEY (tranzactie_rezervare) REFERENCES rezultate(domain_id) DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX idx_oferta_stare ON oferte(stare);
CREATE INDEX idx_interes_client ON interese(client_id);
CREATE INDEX idx_vizionare_agent_data ON vizionari(agent_id, data_ora);
CREATE INDEX idx_rezultat_interes ON rezultate(interes_id);
CREATE UNIQUE INDEX uq_oferta_tranzactie_rezervare ON oferte(tranzactie_rezervare) WHERE tranzactie_rezervare IS NOT NULL;
-- Durable marker and domain IDs allow demo initialization to survive restarts.
CREATE TABLE demo_dataset (
                              id integer PRIMARY KEY CHECK (id = 1),
                              data_referinta date NOT NULL,
                              proprietar_id integer NOT NULL,
                              cumparator_id integer NOT NULL,
                              zona_copou_id integer NOT NULL,
                              agent_vanzare_id integer NOT NULL,
                              oferta_vanzare_id integer NOT NULL,
                              cerere_cumparare_id integer NOT NULL,
                              oferta_inchiriere_id integer NOT NULL,
                              interes_inchiriere_id integer NOT NULL,
                              vizionare_inchiriere_id integer NOT NULL,
                              agent_inchiriere_id integer NOT NULL
);