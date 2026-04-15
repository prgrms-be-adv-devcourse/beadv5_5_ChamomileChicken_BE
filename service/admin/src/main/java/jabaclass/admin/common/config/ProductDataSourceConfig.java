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
	basePackages = {
		"jabaclass.admin.product.infrastructure.persistence",
		"jabaclass.admin.review.infrastructure.persistence"
	},
	entityManagerFactoryRef = "productEntityManagerFactory",
	transactionManagerRef = "productTransactionManager"
)
public class ProductDataSourceConfig {

	@Bean(name = "productDataSource")
	@ConfigurationProperties(prefix = "datasource.product")
	public DataSource productDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name = "productEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean productEntityManagerFactory(
		@Qualifier("productDataSource") DataSource dataSource
	) {
		LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
		factory.setDataSource(dataSource);
		factory.setPackagesToScan(
			"jabaclass.admin.product.domain.model",
			"jabaclass.admin.review.domain.model"
		);
		factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		factory.setJpaPropertyMap(Map.of(
			"hibernate.hbm2ddl.auto", "none",
			"hibernate.physical_naming_strategy",
			"org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
		));
		return factory;
	}

	@Bean(name = "productTransactionManager")
	public PlatformTransactionManager productTransactionManager(
		@Qualifier("productEntityManagerFactory") LocalContainerEntityManagerFactoryBean factory
	) {
		return new JpaTransactionManager(factory.getObject());
	}
}
