package jabaclass.admin.common.config;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
	basePackages = {
		"jabaclass.admin.user.infrastructure.persistence",
		"jabaclass.admin.product.infrastructure.persistence",
		"jabaclass.admin.order.infrastructure.persistence",
		"jabaclass.admin.settlement.infrastructure.persistence",
		"jabaclass.admin.review.infrastructure.persistence"
	},
	entityManagerFactoryRef = "adminEntityManagerFactory",
	transactionManagerRef = "adminTransactionManager"
)
public class AdminDataSourceConfig {

	@Value("${jpa.ddl-auto:none}")
	private String ddlAuto;

	@Primary
	@Bean(name = "adminDataSource")
	@ConfigurationProperties(prefix = "datasource")
	public DataSource adminDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Primary
	@Bean(name = "adminEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean adminEntityManagerFactory(DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
		factory.setDataSource(dataSource);
		factory.setPackagesToScan(
			"jabaclass.admin.user.domain.model",
			"jabaclass.admin.product.domain.model",
			"jabaclass.admin.order.domain.model",
			"jabaclass.admin.settlement.domain.model",
			"jabaclass.admin.review.domain.model"
		);
		factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		factory.setJpaPropertyMap(Map.of(
			"hibernate.hbm2ddl.auto", ddlAuto,
			"hibernate.physical_naming_strategy",
			"org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
		));
		return factory;
	}

	@Primary
	@Bean(name = "adminTransactionManager")
	public PlatformTransactionManager adminTransactionManager(
		LocalContainerEntityManagerFactoryBean adminEntityManagerFactory
	) {
		return new JpaTransactionManager(adminEntityManagerFactory.getObject());
	}
}