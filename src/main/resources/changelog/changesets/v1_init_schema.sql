--liquibase formatted SQL
--changeset chaika:1

CREATE TABLE items (
    id BIGSERIAL NOT NULL,
    name       VARCHAR(255)   NOT NULL,
    price      NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_items PRIMARY KEY (id)
);

-- 'orders' table
CREATE TABLE orders (
    id BIGSERIAL NOT NULL,
    user_id     BIGINT         NOT NULL,
    status      VARCHAR(50)    NOT NULL,
    total_price NUMERIC(10, 2) NOT NULL,
    deleted     BOOLEAN DEFAULT FALSE NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_orders PRIMARY KEY (id)
);

-- 'order_items' table
CREATE TABLE order_items (
    id BIGSERIAL NOT NULL,
    order_id   BIGINT  NOT NULL,
    item_id    BIGINT  NOT NULL,
    quantity   INTEGER NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_on_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_on_item FOREIGN KEY (item_id) REFERENCES items (id)
);


-- Indexes
CREATE INDEX idx_orders_user_id ON orders (user_id);

-- Composite index
CREATE INDEX idx_orders_lookup_perf ON orders (deleted, status, created_at);

-- FK
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_item_id ON order_items (item_id);