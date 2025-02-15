package gaia3d.service.impl;

import gaia3d.config.PropertiesConfig;
import gaia3d.domain.common.QueueMessage;
import gaia3d.domain.common.TileMessage;
import gaia3d.service.AMQPPublishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Cheon JeongDae
 *
 */
@Slf4j
@Service
public class AMQPPublishServiceImpl implements AMQPPublishService {

	@Autowired
	private PropertiesConfig propertiesConfig;
	
	@Autowired
	private RabbitTemplate rabbitTemplate;
	
	@Transactional
	public void send(String exchange, String routingKey, QueueMessage queueMessage) {
		log.info("@@ Publish send message >>> {}", queueMessage);
//		rabbitTemplate.convertAndSend(propertiesConfig.getQueueName(), queueMessage);
		rabbitTemplate.convertAndSend(exchange, routingKey, queueMessage);
	}

	/**
	 * 메시지 전송
	 * @param exchange
	 * @param routingKey
	 * @param tileMessage
	 */
	@Transactional
	public void tileMessageSend(String exchange, String routingKey, TileMessage tileMessage) {
		log.info("@@ Publish send tileMessage >>> {}", tileMessage);
//		rabbitTemplate.convertAndSend(propertiesConfig.getQueueName(), queueMessage);
		rabbitTemplate.convertAndSend(exchange, routingKey, tileMessage);
}
}
