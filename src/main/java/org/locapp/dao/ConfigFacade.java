package org.locapp.dao;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Query;

import org.locapp.dto.Config;
import org.locapp.util.ValidationHelper;

/**
 *
 * @author Bjoern.Buchholz
 *
 */
public class ConfigFacade extends AbstractFacade<Config> {

	private static final Logger LOGGER = Logger.getLogger(ConfigFacade.class.getName());

	public ConfigFacade() {
		super(Config.class);
	}

	public String[] getValue(String key) throws DatabaseException {

		String[] values = new String[] {};
		getEntityManager().getTransaction().begin();
		String queryStr = "select target.value from " + Config.class.getSimpleName() + " target where target.key =:key";
		Query query = getEntityManager().createQuery(queryStr);
		query.setParameter("key", key);
		@SuppressWarnings("rawtypes")
		List resultList = query.getResultList();
		if (resultList != null && resultList.size() > 0)
			values = resultList.get(0).toString().split(",");
		// String value = (String) query.getSingleResult();
		getEntityManager().getTransaction().commit();
		return values;
	}

	public void saveConfig(List<Config> configs) throws DatabaseException {
		try {
			getEntityManager().getTransaction().begin();
			for (Config config : configs) {
				ValidationHelper.validateBean(config);
				getEntityManager().persist(config);
			}
			getEntityManager().getTransaction().commit();

		} catch (Exception e) {
			getLogger().log(Level.SEVERE, e.getMessage(), e);
			if (getEntityManager().getTransaction().isActive()) {
				getEntityManager().getTransaction().setRollbackOnly();
			}
			throw new DatabaseException(e.getMessage(), e);
		}
	}
}
