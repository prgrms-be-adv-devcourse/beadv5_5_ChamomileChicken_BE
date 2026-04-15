package jabaclass.admin.common.config;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
	basePackages = "jabaclass.admin.order.infrastructure.persistence",
	entityManagerFactoryRef = "orderEntityManagerFactory",
	transactionManagerRef = "orderTransactionManager"
)
public class OrderDataSourceConfig {

	@Bean(name = "orderDataSource")
	@ConfigurationProperties(prefix = "datasource.order")
	public DataSource orderDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name = "orderEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean orderEntityManagerFactory(
		@Qualifier("orderDataSource") DataSource dataSource
	) {
		LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
		factory.setDataSource(dataSource);
		factory.setPackagesToScan("jabaclass.admin.order.domain.model");
		factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		factory.setJpaPropertyMap(Map.of(
			"hibernate.hbm2ddl.auto", "none",
			"hibernate.physical_naming_strategy",
			"org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
		));
		return factory;
	}

	@Bean(name = "orderTransactionManager")
	public PlatformTransactionManager orderTransactionManager(
		@Qualifier("orderEntityManagerFactory") LocalContainerEntityManagerFactoryBean factory
	) {
		return new JpaTransactionManager(factory.getObject());
	}
}
