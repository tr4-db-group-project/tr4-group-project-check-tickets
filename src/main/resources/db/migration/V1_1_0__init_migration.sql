CREATE TABLE IF NOT EXISTS events (
    id uuid NOT NULL PRIMARY KEY,
    name varchar(255),
    numberoftickets INT,
    email varchar(255)
    );

-- todo remove email from db