package jabaclass.admin.common.config;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
	basePackages = "jabaclass.admin.settlement.infrastructure.persistence",
	entityManagerFactoryRef = "settlementEntityManagerFactory",
	transactionManagerRef = "settlementTransactionManager"
)
public class SettlementDataSourceConfig {

	@Value("${jpa.ddl-auto:none}")
	private String ddlAuto;

	@Bean(name = "settlementDataSource")
	@ConfigurationProperties(prefix = "datasource.settlement")
	public DataSource settlementDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name = "settlementEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean settlementEntityManagerFactory(
		@Qualifier("settlementDataSource") DataSource dataSource
	) {
		LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
		factory.setDataSource(dataSource);
		factory.setPackagesToScan("jabaclass.admin.settlement.domain.model");
		factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		factory.setJpaPropertyMap(Map.of(
			"hibernate.hbm2ddl.auto", ddlAuto,
			"hibernate.physical_naming_strategy",
			"org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
		));
		return factory;
	}

	@Bean(name = "settlementTransactionManager")
	public PlatformTransactionManager settlementTransactionManager(
		@Qualifier("settlementEntityManagerFactory") LocalContainerEntityManagerFactoryBean factory
	) {
		return new JpaTransactionManager(factory.getObject());
	}
}
