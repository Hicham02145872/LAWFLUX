package ma.lawflux.cases.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ma.lawflux.cases.domain.Client;
import ma.lawflux.cases.domain.ClientRepository;
import ma.lawflux.cases.domain.ClientStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcClientRepository implements ClientRepository {
    private final JdbcTemplate jdbc;

    public JdbcClientRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Client> findById(UUID cabinetId, UUID clientId) {
        return jdbc.query(
                """
                SELECT id, cabinet_id, name, email, phone, address,
                       status, created_at, updated_at, archived_at
                FROM clients
                WHERE cabinet_id = ? AND id = ?
                """,
                JdbcClientRepository::mapClient,
                cabinetId,
                clientId
        ).stream().findFirst();
    }

    @Override
    public List<Client> findPage(
            UUID cabinetId,
            ClientStatus status,
            int limit,
            int offset
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, cabinet_id, name, email, phone, address,
                       status, created_at, updated_at, archived_at
                FROM clients
                WHERE cabinet_id = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(cabinetId);

        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status.name());
        }

        sql.append(" ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);

        return jdbc.query(sql.toString(), JdbcClientRepository::mapClient, args.toArray());
    }

    @Override
    public long count(UUID cabinetId, ClientStatus status) {
        StringBuilder sql = new StringBuilder(
                "SELECT count(*) FROM clients WHERE cabinet_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(cabinetId);

        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status.name());
        }

        Long count = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    @Override
    public Client create(
            UUID cabinetId,
            String name,
            String email,
            String phone,
            String address
    ) {
        UUID id = UUID.randomUUID();

        return jdbc.queryForObject(
                """
                INSERT INTO clients
                    (id, cabinet_id, name, email, phone, address, status)
                VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')
                RETURNING id, cabinet_id, name, email, phone, address,
                          status, created_at, updated_at, archived_at
                """,
                JdbcClientRepository::mapClient,
                id, cabinetId, name, email, phone, address
        );
    }

    @Override
    public boolean update(
            UUID cabinetId,
            UUID clientId,
            String name,
            String email,
            String phone,
            String address
    ) {
        return jdbc.update(
                """
                UPDATE clients
                SET name = ?, email = ?, phone = ?, address = ?, updated_at = now()
                WHERE cabinet_id = ? AND id = ? AND status = 'ACTIVE'
                """,
                name, email, phone, address, cabinetId, clientId
        ) > 0;
    }

    @Override
    public boolean archive(UUID cabinetId, UUID clientId) {
        return jdbc.update(
                """
                UPDATE clients
                SET status = 'ARCHIVED', archived_at = now(), updated_at = now()
                WHERE cabinet_id = ? AND id = ? AND status = 'ACTIVE'
                """,
                cabinetId, clientId
        ) > 0;
    }

    private static Client mapClient(ResultSet rs, int rowNum) throws SQLException {
        Timestamp archivedAt = rs.getTimestamp("archived_at");

        return new Client(
                rs.getObject("id", UUID.class),
                rs.getObject("cabinet_id", UUID.class),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("address"),
                ClientStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                archivedAt == null ? null : archivedAt.toInstant()
        );
    }
}