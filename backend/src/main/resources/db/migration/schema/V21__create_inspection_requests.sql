CREATE TABLE inspection_requests (
                                     id CHAR(36) NOT NULL,
                                     production_lot_id CHAR(36) NOT NULL,
                                     inspection_unit VARCHAR(255) NOT NULL,
                                     sample_sent_date DATE NOT NULL,
                                     status VARCHAR(30) NOT NULL,
                                     created_by CHAR(36) NOT NULL,
                                     created_at DATETIME NOT NULL,
                                     updated_at DATETIME NULL,

                                     PRIMARY KEY (id),

                                     CONSTRAINT fk_inspection_request_lot
                                         FOREIGN KEY (production_lot_id)
                                             REFERENCES production_lot(id),

                                     CONSTRAINT fk_inspection_request_created_by
                                         FOREIGN KEY (created_by)
                                             REFERENCES users(user_id)
);