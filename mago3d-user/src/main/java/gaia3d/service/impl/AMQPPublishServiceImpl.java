package gaia3d.service.impl;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gaia3d.config.PropertiesConfig;
import gaia3d.domain.common.QueueMessage;
import gaia3d.service.AMQPPublishService;
import lombok.extern.slf4j.Slf4j;

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
		rabbitTemplate.convertAndSend(exchange, routingKey, queueMessage);
	}
}
