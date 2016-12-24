package be.yannickdeturck.lagomshop.item.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Yannick De Turck
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = Void.class)
@JsonSubTypes({
        @JsonSubTypes.Type(ItemEvent.ItemCreated.class)
})
public interface ItemEvent {
    UUID getId();

    @JsonTypeName("item-created")
    final class ItemCreated implements ItemEvent {
        private final UUID id;
        private final String name;
        private final BigDecimal price;

        @JsonCreator
        public ItemCreated(UUID id, String name, BigDecimal price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        @Override
        public UUID getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public BigDecimal getPrice() {
            return price;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ItemCreated that = (ItemCreated) o;

            if (id != null ? !id.equals(that.id) : that.id != null) return false;
            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            return price != null ? price.equals(that.price) : that.price == null;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (price != null ? price.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ItemCreated{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", price=" + price +
                    '}';
        }
    }
}