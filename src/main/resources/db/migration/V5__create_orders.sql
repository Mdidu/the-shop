CREATE TABLE orders (
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID           NOT NULL REFERENCES users(id),
    status            VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    total_amount      NUMERIC(10, 2) NOT NULL,

    -- Adresse de livraison embarquée : une commande a une seule adresse, immuable
    shipping_street   VARCHAR(255) NOT NULL,
    shipping_city     VARCHAR(100) NOT NULL,
    shipping_zip_code VARCHAR(20)  NOT NULL,
    shipping_country  VARCHAR(100) NOT NULL,

    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT orders_status_check CHECK (status IN (
        'PENDING', 'CONFIRMED', 'PREPARING', 'SHIPPED', 'DELIVERED',
        'CANCELLED', 'RETURN_REQUESTED', 'RETURNED'
    ))
);

CREATE TABLE order_items (
    id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id      UUID           NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id    UUID           NOT NULL REFERENCES products(id),
    product_name  VARCHAR(255)   NOT NULL,
    quantity      INTEGER        NOT NULL,
    unit_price    NUMERIC(10, 2) NOT NULL,

    CONSTRAINT order_items_quantity_check CHECK (quantity > 0)
);

-- Historique complet des transitions d'état pour auditabilité
CREATE TABLE order_status_history (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id         UUID        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    previous_status  VARCHAR(30),
    new_status       VARCHAR(30) NOT NULL,
    changed_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id              ON orders(user_id);
CREATE INDEX idx_orders_status               ON orders(status);
CREATE INDEX idx_order_items_order_id        ON order_items(order_id);
CREATE INDEX idx_order_status_history_order  ON order_status_history(order_id);
