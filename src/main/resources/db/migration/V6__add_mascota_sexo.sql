ALTER TABLE mascota
    ADD COLUMN sexo VARCHAR(20) NOT NULL DEFAULT 'Macho'
    CHECK (sexo IN ('Hembra', 'Macho'));
