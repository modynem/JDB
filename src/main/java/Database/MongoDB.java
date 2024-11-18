package Database;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;

import java.util.Date;
import java.util.Map;

public class MongoDB extends ListenerAdapter {
    private final MongoClient mongoClient;
    private final String databaseName;
    private final String collectionName;
    private MongoCollection<Document> collection;

    public MongoDB(String mongoUri, String databaseName, String CollectionName) {
        try {
            this.collectionName = CollectionName;
            this.databaseName = databaseName;
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(mongoUri))
                    .serverApi(ServerApi.builder()
                            .version(ServerApiVersion.V1)
                            .build())
                    .build();
            this.mongoClient = MongoClients.create(settings);

            // Initialize collection
            this.collection = getCollection();

            // Create indexes if they don't exist
            createIndexes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MongoDB connection: " + e.getMessage());
        }
    }

    private void createIndexes() {
        try {
            // Create compound index for userId and guildId
            collection.createIndex(
                    Indexes.compoundIndex(
                            Indexes.ascending("userId"),
                            Indexes.ascending("guildId")
                    ),
                    new IndexOptions().unique(true)
            );
        } catch (MongoException e) {
            e.printStackTrace();
        }
    }

    public MongoCollection<Document> getCollection() {
        if (collection == null) {
            try {
                MongoDatabase database = mongoClient.getDatabase(databaseName);
                collection = database.getCollection(collectionName);

                // Verify connection
                collection.countDocuments();

                return collection;
            } catch (MongoException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to connect to MongoDB collection: " + e.getMessage());
            }
        }
        return collection;
    }

    public Document getDoc(String userId, String guildId) {
        try {
            return collection.find(
                    Filters.and(
                            Filters.eq("userId", userId),
                            Filters.eq("guildId", guildId)
                    )
            ).first();
        } catch (MongoException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void makeNewData(Member member, Guild guild) {
        try {
            // First check if the user already has data
            Document existingData = collection.find(
                    Filters.and(
                            Filters.eq("userId", member.getId()),
                            Filters.eq("guildId", guild.getId())
                    )
            ).first();

            // Only create new data if none exists
            if (existingData == null) {
                Document newMember = new Document()
                        .append("userId", member.getId())
                        .append("guildId", guild.getId())
                        .append("warns", 0)
                        .append("createdAt", new Date())
                        .append("username", member.getUser().getName());

                collection.insertOne(newMember);
            }
        } catch (MongoException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create new member data: " + e.getMessage());
        }
    }

    public void makeNewSetupData(Guild guild, Map setupStates) {
        try {
            // First check if the user already has data
            Document existingData = collection.find(
                    Filters.and(
                            Filters.eq("guildId", guild.getId())
                    )
            ).first();

            if (existingData == null) {
                Document newMember = new Document()
                        .append("notificationsChannelId", setupStates.get("notificationChannelId"))
                        .append("for_hireChannelId", setupStates.get("for_hireChannelId"))
                        .append("hiringChannelId", setupStates.get("hiringChannelId"))
                        .append("sellingChannelId", setupStates.get("sellingChannelId"))
                        .append("guildId", guild.getId());

                collection.insertOne(newMember);
            } else {
                try {
                    collection.updateMany(
                            Filters.and(
                                    Filters.eq("guildId", guild.getId())
                            ),
                            Updates.combine(
                                    Updates.set("notificationsChannelId", setupStates.get("notificationChannelId")),
                                    Updates.set("for_hireChannelId", setupStates.get("for_hireChannelId")),
                                    Updates.set("hiringChannelId", setupStates.get("hiringChannelId")),
                                    Updates.set("sellingChannelId", setupStates.get("sellingChannelId"))
                            )
                    );
                } catch (MongoException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to update guild setup data: " + e.getMessage());
                }
            }
        } catch (MongoException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create new guild data: " + e.getMessage());
        }
    }

    public void updateWarns(String userId, String guildId, int newWarns) {
        try {
            collection.updateOne(
                    Filters.and(
                            Filters.eq("userId", userId),
                            Filters.eq("guildId", guildId)
                    ),
                    Updates.set("warns", newWarns)
            );
        } catch (MongoException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to update warns: " + e.getMessage());
        }
    }

    public void close() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}