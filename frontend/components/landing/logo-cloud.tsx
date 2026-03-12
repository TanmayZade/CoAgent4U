export function LogoCloud() {
  const stats = [
    { value: "20 days", label: "saved on daily builds.", company: "Enterprise Teams" },
    { value: "98%", label: "faster time to market.", company: "Startups" },
    { value: "300%", label: "increase in productivity.", company: "Agencies" },
    { value: "6x", label: "faster to coordinate.", company: "Remote Teams" },
  ]

  return (
    <section className="py-16 border-y border-border bg-muted/30">
      <div className="mx-auto max-w-7xl px-6 lg:px-8">
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-8 lg:gap-4">
          {stats.map((stat, i) => (
            <div key={i} className="text-center lg:text-left lg:border-l lg:first:border-l-0 border-border lg:pl-8 lg:first:pl-0">
              <p className="text-foreground">
                <span className="text-2xl font-bold">{stat.value}</span>{" "}
                <span className="text-muted-foreground">{stat.label}</span>
              </p>
              <p className="mt-2 text-sm font-medium text-muted-foreground">{stat.company}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
