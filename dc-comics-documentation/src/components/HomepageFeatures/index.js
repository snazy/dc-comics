import React from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';

const FeatureList = [
  {
    title: 'Light and fast',
    icon: <img src="img/deadline.png" width="60px" />,
    description: (
      <>
        DC Comics is powered by <a href="https://quarkus.io">Quarkus</a>, the supersonic/subatomic Java framework. 
      </>
    ),
  },
  {
    title: 'Cloud & GraalVM',
    icon: <img src="img/cloud.png" width="60px" />,
    description: (
      <>
        DC Comics is cloud native, with seamless integration with Kubernetes. It also support GraalVM natively to bootstrap efficienly.
      </>
    ),
  },
  {
    title: 'Turnkey extension',
    icon: <img src="img/deal.png" width="60px" />,
    description: (
      <>
        DC Comics brings a single extension that bring all features/extensions needed for Dremio services (structured logging, telemetry, gRPC, ...). The extensions are lazy loaded meaning that they are actually loaded only when the service is concretely using it.
      </>
    ),
  },
];

function Feature({icon, title, description}) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center">
        {icon}
      </div>
      <div className="text--center padding-horiz--md">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
