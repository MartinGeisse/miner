package name.martingeisse.miner.server.entities;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QPlayerAwardedAchievement is a Querydsl query type for PlayerAwardedAchievement
 */
@Generated("de.servicereisen.companion.tools.sql.MyMetaDataSerializer")
@SuppressWarnings("all")
public class QPlayerAwardedAchievement extends com.querydsl.sql.RelationalPathBase<PlayerAwardedAchievement> {

    private static final long serialVersionUID = 66512714;

    public static final QPlayerAwardedAchievement PlayerAwardedAchievement = new QPlayerAwardedAchievement("PlayerAwardedAchievement");

    public final StringPath achievementCode = createString("achievementCode");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> playerId = createNumber("playerId", Long.class);

    public final com.querydsl.sql.PrimaryKey<PlayerAwardedAchievement> playerAwardedAchievementPkey = createPrimaryKey(id);

    public final com.querydsl.sql.ForeignKey<Player> playerAwardedAchievementPlayerIdFkey = createForeignKey(playerId, "id");

    public QPlayerAwardedAchievement(String variable) {
        super(PlayerAwardedAchievement.class, forVariable(variable), "miner", "PlayerAwardedAchievement");
        addMetadata();
    }

    public QPlayerAwardedAchievement(String variable, String schema, String table) {
        super(PlayerAwardedAchievement.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QPlayerAwardedAchievement(Path<? extends PlayerAwardedAchievement> path) {
        super(path.getType(), path.getMetadata(), "miner", "PlayerAwardedAchievement");
        addMetadata();
    }

    public QPlayerAwardedAchievement(PathMetadata metadata) {
        super(PlayerAwardedAchievement.class, metadata, "miner", "PlayerAwardedAchievement");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(achievementCode, ColumnMetadata.named("achievementCode").withIndex(3).ofType(Types.VARCHAR).withSize(255).notNull());
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT).withSize(19).notNull());
        addMetadata(playerId, ColumnMetadata.named("playerId").withIndex(2).ofType(Types.BIGINT).withSize(19).notNull());
    }

}

