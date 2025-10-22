-- Top 10 by unit / current balance

SELECT address, quantity
             FROM address_balance_current
             WHERE unit = '279c909f348e533da5808898f87f9a14bb2c3dfbbacccd631d927a3f534e454b'
               AND quantity > 0
             ORDER BY quantity DESC
             LIMIT 10;

-- :policy_id := 56-hex string (no 0x prefix)
SELECT address, SUM(quantity) AS total_qty
FROM address_balance_current
WHERE LEFT(unit, 56) = '4523c5e21d409b81c95b45b0aea275b8ea1406e6cafea5583b9f8a5f'
  AND quantity > 0
GROUP BY address
ORDER BY total_qty DESC
LIMIT 10;

-- List all holders of policy id
SELECT DISTINCT address
FROM address_balance_current
WHERE LEFT(unit, 56) = '4523c5e21d409b81c95b45b0aea275b8ea1406e6cafea5583b9f8a5f'
  AND quantity > 0;

-- Rank addresses by number of tokens (units) held under a policy
SELECT address, COUNT(*) AS tokens_held
FROM yaci_store.address_balance_current
WHERE LEFT(unit, 56) = '4523c5e21d409b81c95b45b0aea275b8ea1406e6cafea5583b9f8a5f'
  AND quantity > 0
GROUP BY address
ORDER BY tokens_held DESC
LIMIT 10;
