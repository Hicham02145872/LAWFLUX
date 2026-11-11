package ma.lawflux.notifications.infrastructure;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration(proxyBeanMethods = false)
public class ScaffoldRabbitTopology {
    public static final String EXCHANGE = "lawflux.scaffold";
    public static final String QUEUE = "lawflux.scaffold.notifications";
    public static final String KEY = "scaffold.ping";

    @Bean
    RejectAndDontRequeueRecoverer scaffoldMessageRecoverer() {
        return new RejectAndDontRequeueRecoverer();
    }

    @Bean
    Declarables scaffoldTopology() {
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        DirectExchange dead = new DirectExchange(EXCHANGE + ".dlx", true, false);
        Queue queue = QueueBuilder.durable(QUEUE).deadLetterExchange(dead.getName()).deadLetterRoutingKey("failed").build();
        Queue dlq = QueueBuilder.durable(QUEUE + ".dlq").build();
        return new Declarables(exchange, dead, queue, dlq,
            BindingBuilder.bind(queue).to(exchange).with(KEY),
            BindingBuilder.bind(dlq).to(dead).with("failed"));
    }
}
